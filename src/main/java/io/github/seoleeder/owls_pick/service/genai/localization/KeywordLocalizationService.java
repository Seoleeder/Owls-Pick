package io.github.seoleeder.owls_pick.service.genai.localization;

import io.github.seoleeder.owls_pick.dto.request.KeywordLocalizationRequest;
import io.github.seoleeder.owls_pick.dto.response.KeywordLocalizationBulkResponse;
import io.github.seoleeder.owls_pick.entity.game.KeywordDictionary;
import io.github.seoleeder.owls_pick.entity.game.Tag;
import io.github.seoleeder.owls_pick.entity.genai.GenaiFailedTask;
import io.github.seoleeder.owls_pick.entity.genai.enums.GenaiFailReason;
import io.github.seoleeder.owls_pick.entity.genai.enums.GenaiPipelineType;
import io.github.seoleeder.owls_pick.global.config.properties.GenaiProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.GenaiFailedTaskRepository;
import io.github.seoleeder.owls_pick.repository.KeywordDictionaryRepository;
import io.github.seoleeder.owls_pick.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeywordLocalizationService {

    private final KeywordDictionaryRepository dictionaryRepository;
    private final TagRepository tagRepository;
    private final RestClient localizationRestClient;
    private final GenaiProperties props;
    private final TransactionTemplate transactionTemplate;
    private final GenaiFailedTaskRepository failedTaskRepository;

    public KeywordLocalizationService(
            KeywordDictionaryRepository dictionaryRepository,
            TagRepository tagRepository,
            @Qualifier("genaiRestClient") RestClient localizationRestClient,
            GenaiProperties props,
            TransactionTemplate transactionTemplate,
            GenaiFailedTaskRepository failedTaskRepository) {

        this.dictionaryRepository = dictionaryRepository;
        this.tagRepository = tagRepository;
        this.localizationRestClient = localizationRestClient;
        this.props = props;
        this.transactionTemplate = transactionTemplate;
        this.failedTaskRepository = failedTaskRepository;
    }

    // 비동기 콜백 상태 관리를 위한 인메모리 맵
    private final Map<String, CompletableFuture<KeywordLocalizationBulkResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 환경 변수에 설정된 기본 청크 사이즈를 사용하여 파이프라인 전체 실행
     */
    public int runPipeline() {
        return runPipeline(props.localization().keyword().chunkSize(), false);
    }

    /**
     * 키워드 한글화 파이프라인 전체 실행
     */
    public int runPipeline(int chunkSize, boolean isSingleRun) {
        log.info("[GenAI] Starting Keyword Localization Pipeline with chunk size {}...", chunkSize);
        // 신규 영문 키워드 추출 및 사전 등록
        extractNewKeywords();

        // 영문 키워드를 청크 단위로 분할하여 한글화 실행
        int processedCount = processUnlocalizedKeywords(chunkSize, isSingleRun);

        // 한글화된 키워드 동기화
        int mappedTagCount = applyLocalizationsToTags(props.localization().keyword().dbBatchSize());

        log.info("[GenAI] Keyword Localization Pipeline Completed. AI Translated: {}, Tags Mapped: {}", processedCount, mappedTagCount);
        return processedCount;
    }

    /**
     * Tag 엔티티에서 고유 영문 키워드를 추출하여 키워드 사전에 등록
     */
    private void extractNewKeywords() {
        transactionTemplate.executeWithoutResult(status -> {
            // 모든 고유 영문 키워드를 중복 없이 조회
            List<String> distinctKeywords = tagRepository.findAllDistinctKeywords();
            if (distinctKeywords.isEmpty()) return;

            // 이미 키워드 사전에 존재하는 영문 키워드 조회
            List<String> existingKeywords = dictionaryRepository.findExistingEngNames(distinctKeywords);

            // 키워드 탐색 성능 최적화를 위한 HashSet 변환
            Set<String> existingSet = new HashSet<>(existingKeywords);

            // 사전에 없는 신규 키워드만 필터링하여 엔티티로 변환
            List<KeywordDictionary> newDictionaries = distinctKeywords.stream()
                    .filter(kw -> !existingSet.contains(kw))
                    .map(eng -> KeywordDictionary.builder().engName(eng).build())
                    .toList();

            if (!newDictionaries.isEmpty()) {
                // 신규 키워드 목록을 키워드 사전에 저장
                dictionaryRepository.saveAll(newDictionaries);
                log.info("[GenAI] Inserted {} new keywords into Dictionary.", newDictionaries.size());
            }
        });
    }

    /**
     * 환경 변수에 설정된 청크 사이즈를 사용하여 키워드 한글화 실행
     */
    public void processUnlocalizedKeywords() {
        processUnlocalizedKeywords(props.localization().keyword().chunkSize(), false);
    }

    /**
     * 지정된 단위(Chunk)로 영문 키워드를 분할하여 한글화 실행
     */
    public int processUnlocalizedKeywords(int chunkSize, boolean isSingleRun) {

        // 데이터 사전에서 한글화되지 않은 영문 키워드 조회
        List<KeywordDictionary> unlocalizedEntities = dictionaryRepository.findUnlocalizedKeywords();
        if (unlocalizedEntities.isEmpty()) {
            log.debug("[GenAI] No unlocalized keywords found. Task skipped.");
            return 0;
        }

        log.info("[GenAI] Found {} unlocalized keywords. Processing in chunks of {}.", unlocalizedEntities.size(), chunkSize);

        int totalProcessed = 0;

        for (int i = 0; i < unlocalizedEntities.size(); i += chunkSize) {
            List<KeywordDictionary> chunk = unlocalizedEntities.subList(i, Math.min(unlocalizedEntities.size(), i + chunkSize));

            try {
                processLocalizationChunk(chunk);
                totalProcessed += chunk.size();
            } catch (Exception e) {
                // 통신 장애 발생 시 청크 데이터를 실패 작업으로 기록하여 무한 루프 방지
                log.error("[GenAI] Failed to process keyword localization chunk. Skipping to next chunk.", e);
                Map<Long, GenaiFailReason> failedTaskMap = chunk.stream()
                        .collect(Collectors.toMap(KeywordDictionary::getId, entity -> GenaiFailReason.NETWORK_ERROR));
                recordFailedTasks(failedTaskMap);
                totalProcessed += chunk.size();
            }

            if (isSingleRun) {
                break;
            }

            if (i + chunkSize < unlocalizedEntities.size()) {
                try {
                    Thread.sleep(props.localization().delayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[GenAI] Keyword Localization Chunk processing interrupted", e);
                    break;
                }
            }
        }

        return totalProcessed;
    }

    /**
     * 단일 Chunk 단위의 키워드 한글화 파이프라인 실행 및 DB 업데이트
     */
    private void processLocalizationChunk(List<KeywordDictionary> chunkEntities) {
        // 요청 DTO 조립
        KeywordLocalizationRequest request = buildRequestDto(chunkEntities);

        // 키워드 한글화 엔진 통신
        KeywordLocalizationBulkResponse response = sendToAiEngine(request);

        // 결과 DB 반영
        transactionTemplate.executeWithoutResult(status -> {
            // 영속 상태의 사전 엔티티 일괄 재조회
            List<Long> ids = chunkEntities.stream().map(KeywordDictionary::getId).toList();
            List<KeywordDictionary> managedEntities = dictionaryRepository.findAllById(ids);

            // 한글화 결과 매핑 및 상태 업데이트
            applyLocalizationResults(managedEntities, response);
        });
    }

    /**
     * 한글화가 완료된 키워드 사전을 가지고 Tag의 한글 키워드 배열(keywordsKo) 업데이트
     */
    private int applyLocalizationsToTags(int dbBatchSize) {

        // 한글화 완료된 키워드 사전 데이터를 Map으로 로드
        Map<String, String> dictionaryMap = dictionaryRepository.findAll().stream()
                .filter(dict -> dict.getKorName() != null)
                .collect(Collectors.toMap(
                        KeywordDictionary::getEngName,
                        KeywordDictionary::getKorName
                ));

        if (dictionaryMap.isEmpty()) {
            return 0;
        }

        int totalMapped = 0;

        // 대용량 Tag 엔티티 청크 단위 조회 및 처리 루프
        while (true) {

            Integer mappedCount = transactionTemplate.execute(status -> {

                // 업데이트가 필요한 Tag 엔티티를 배치 크기만큼 조회
                List<Tag> targetTags = tagRepository.findTagsNeedingKeywordLocalization(dbBatchSize);
                if (targetTags.isEmpty()) {
                    return 0; // 더 이상 업데이트할 태그가 없으면 종료
                }

                // 영문 키워드를 한글 사전과 매핑하여 태그 업데이트
                for (Tag tag : targetTags) {
                    List<String> localizedKeywords = tag.getKeywords().stream()
                            .map(eng -> dictionaryMap.getOrDefault(eng, eng))
                            .toList();

                    tag.updateKeywordsKo(localizedKeywords);
                }
                return targetTags.size();
            });

            if (mappedCount == null || mappedCount == 0) {
                break;
            }

            totalMapped += mappedCount;
            log.info("Successfully applied translated keywords to {} tags. (Total: {})", mappedCount, totalMapped);
        }

        return totalMapped;
    }

    /**
     * 키워드 한글화 실패 작업 재시도
     */
    public void retryFailedTasks() {
        // 아직 조치되지 않은 키워드 실패 내역 조회
        List<GenaiFailedTask> failedTasks = failedTaskRepository.findUnhandledTasks(GenaiPipelineType.KEYWORD_LOCALIZATION);
        if (failedTasks.isEmpty()) {
            return;
        }

        log.info("[GenAI] Retrying {} failed Keyword Localization tasks...", failedTasks.size());

        // 사전에 정의된 청크 사이즈 할당
        int chunkSize = props.localization().keyword().chunkSize();

        // 대량 실패 건 방어를 위한 청크 단위 분할 처리
        for (int i = 0; i < failedTasks.size(); i += chunkSize) {
            List<GenaiFailedTask> taskChunk = failedTasks.subList(i, Math.min(failedTasks.size(), i + chunkSize));
            List<Long> dictionaryIds = taskChunk.stream().map(GenaiFailedTask::getTargetId).toList();

            try {
                // 실패 대상 키워드 엔티티 조회
                List<KeywordDictionary> targetKeywords = dictionaryRepository.findAllById(dictionaryIds);

                // 통신 DTO 생성 및 키워드 한글화 재요청
                KeywordLocalizationRequest request = buildRequestDto(targetKeywords);
                KeywordLocalizationBulkResponse response = sendToAiEngine(request);

                // 재시도 결과 적용 및 상태 갱신 트랜잭션 개방
                transactionTemplate.executeWithoutResult(status -> {
                    // 키워드 사전 엔티티 영속화 및 데이터 매핑
                    List<KeywordDictionary> managedEntities = dictionaryRepository.findAllById(dictionaryIds);
                    applyLocalizationResults(managedEntities, response);

                    // 실패 작업 이력 영속화 및 조치 상태(isHandled) 변경
                    List<Long> taskIds = taskChunk.stream().map(GenaiFailedTask::getId).toList();
                    List<GenaiFailedTask> managedTasks = failedTaskRepository.findAllById(taskIds);
                    managedTasks.forEach(GenaiFailedTask::markAsHandled);
                });
            } catch (Exception e) {
                // 청크 단위 재시도 예외 격리
                log.error("[GenAI] Failed to retry Keyword Localization chunk. Skipping to next chunk.", e);
            }
        }
    }

    /**
     * Webhook 수신 시 대기 중인 요청 식별자를 찾아 결과 반환 및 스레드 대기 해제
     */
    public void completePendingTask(KeywordLocalizationBulkResponse response) {
        CompletableFuture<KeywordLocalizationBulkResponse> future = pendingRequests.get(response.requestId());
        if (future != null) {
            future.complete(response);
            log.info("[GenAI] Successfully received callback for Request ID: {}", response.requestId());
        } else {
            log.warn("[GenAI] Received callback for unknown or expired Request ID: {}", response.requestId());
        }
    }

    // ---------------------------------------------------------------------------------
    // Helper Methods
    // ---------------------------------------------------------------------------------

    /**
     * 키워드 사전 엔티티 리스트를 키워드 한글화 요청 DTO로 변환
     */
    private KeywordLocalizationRequest buildRequestDto(List<KeywordDictionary> entities) {
        List<String> keywords = entities.stream()
                .map(KeywordDictionary::getEngName)
                .toList();
        return new KeywordLocalizationRequest(null, null, keywords);
    }

    /**
     * 한글화 엔진으로 HTTP 요청 전송 및 결과 반환
     */
    private KeywordLocalizationBulkResponse sendToAiEngine(KeywordLocalizationRequest request) {

        // 요청 및 콜백 매핑용 고유 식별자 발급
        String requestId = UUID.randomUUID().toString();

        // Webhook URL 생성
        String callbackUrl = props.callbackBaseUrl() + "/api/internal/callback/genai/keywords";

        KeywordLocalizationRequest asyncRequest = new KeywordLocalizationRequest(
                requestId, callbackUrl, request.keywords());

        log.info("[GenAI] Sending bulk keyword localization request for {} keywords to AI Engine...", request.keywords().size());

        URI targetUri = UriComponentsBuilder.fromUriString(props.fastapiUrl())
                .path("/api/localization/keywords/bulk")
                .build()
                .toUri();

        try {
            // 키워드 데이터 전송 직후 커넥션 해제
            localizationRestClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(asyncRequest)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("[GenAI] Initial Communication Error with GenAI Server for Game Localization");
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }

        // 인메모리 스레드 대기 상태 전환
        CompletableFuture<KeywordLocalizationBulkResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            // Webhook 응답 수신 대기
            return future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("[GenAI] Webhook callback timeout for Request ID: {}", requestId);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        } catch (Exception e) {
            log.error("[GenAI] Failed to wait for webhook callback for Request ID: {}", requestId, e);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        } finally {
            // 대기 인메모리 객체 제거
            pendingRequests.remove(requestId);
        }
    }

    /**
     * 키워드 한글화 엔진 응답을 원본 사전 엔티티에 매핑
     */
    private void applyLocalizationResults(List<KeywordDictionary> chunkEntities, KeywordLocalizationBulkResponse response) {
        if (response == null || !response.success() || response.localizationResults() == null || response.localizationResults().isEmpty()) {
            return;
        }

        Map<String, KeywordDictionary> entityMap = chunkEntities.stream()
                .collect(Collectors.toMap(KeywordDictionary::getEngName, e -> e));

        int successCount = 0;
        Map<Long, GenaiFailReason> partialFailures = new HashMap<>();

        for (KeywordLocalizationBulkResponse.KeywordLocalizationResponse result : response.localizationResults()) {
            KeywordDictionary entity = entityMap.get(result.engName());
            if (entity == null) {
                continue;
            }

            // 개별 실패 사유 존재 시 매핑 생략 및 실패 작업 맵에 적재
            if (result.errorReason() != null) {
                GenaiFailReason failReason = parseFailReason(result.errorReason());
                log.warn("[GenAI] AI returned failed status for Keyword: {}. Reason: {}. Recording to FailedTask.", entity.getEngName(), failReason);
                partialFailures.put(entity.getId(), failReason);
                continue;
            }

            // 번역 누락 또는 FAILED 시 영문 원본 보존 방지 및 DLQ 적재 처리
            if (result.korName() == null || result.korName().trim().equalsIgnoreCase(entity.getEngName().trim())) {
                log.warn("[GenAI] Invalid or untranslated korName received for Keyword: {}. Recording to FailedTask.", entity.getEngName());
                partialFailures.put(entity.getId(), GenaiFailReason.INVALID_RESPONSE);
                continue;
            }

            entity.updateLocalization(result.korName());
            successCount++;
        }

        // 부분 실패 건을 1번의 배치 트랜잭션으로 일괄 적재
        if (!partialFailures.isEmpty()) {
            recordFailedTasks(partialFailures);
            log.warn("[GenAI] Recorded {} partial failure keyword tasks to DLQ.", partialFailures.size());
        }

        log.info("[GenAI] Successfully localized {} keywords in current chunk.", successCount);
    }

    /**
     * 실패 사유 문자열을 Enum으로 변환
     */
    private GenaiFailReason parseFailReason(String reasonStr) {
        if (reasonStr == null || reasonStr.isBlank()) {
            return GenaiFailReason.UNKNOWN_ERROR;
        }
        try {
            return GenaiFailReason.valueOf(reasonStr);
        } catch (IllegalArgumentException e) {
            log.warn("[GenAI] Unknown GenaiFailReason received: {}. Falling back to UNKNOWN_ERROR.", reasonStr);
            return GenaiFailReason.UNKNOWN_ERROR;
        }
    }

    /**
     * 추후 재시도 및 통계를 위한 실패 대상 식별자 및 실패 사유 적재
     */
    private void recordFailedTasks(Map<Long, GenaiFailReason> failedTaskMap) {
        if (failedTaskMap.isEmpty()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            List<GenaiFailedTask> failedTasks = failedTaskMap.entrySet().stream()
                    .map(entry -> GenaiFailedTask.builder()
                            .pipelineType(GenaiPipelineType.KEYWORD_LOCALIZATION)
                            .targetId(entry.getKey())
                            .failReason(entry.getValue())
                            .build())
                    .toList();

            failedTaskRepository.saveAll(failedTasks);
        });
    }
}