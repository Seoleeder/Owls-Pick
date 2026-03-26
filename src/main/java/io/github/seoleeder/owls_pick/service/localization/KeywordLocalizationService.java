package io.github.seoleeder.owls_pick.service.localization;

import io.github.seoleeder.owls_pick.dto.request.KeywordLocalizationRequest;
import io.github.seoleeder.owls_pick.dto.response.BulkKeywordLocalizationResponse;
import io.github.seoleeder.owls_pick.entity.game.KeywordDictionary;
import io.github.seoleeder.owls_pick.entity.game.Tag;
import io.github.seoleeder.owls_pick.global.config.properties.LocalizationProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.KeywordDictionaryRepository;
import io.github.seoleeder.owls_pick.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeywordLocalizationService {

    private final KeywordDictionaryRepository dictionaryRepository;
    private final TagRepository tagRepository;
    private final RestClient localizationRestClient;
    private final LocalizationProperties localizationProperties;
    private final TransactionTemplate transactionTemplate;

    public KeywordLocalizationService(
            KeywordDictionaryRepository dictionaryRepository,
            TagRepository tagRepository,
            @Qualifier("localizationRestClient") RestClient localizationRestClient,
            LocalizationProperties localizationProperties,
            TransactionTemplate transactionTemplate) {

        this.dictionaryRepository = dictionaryRepository;
        this.tagRepository = tagRepository;
        this.localizationRestClient = localizationRestClient;
        this.localizationProperties = localizationProperties;
        this.transactionTemplate = transactionTemplate;

    }

    /**
     * 환경 변수에 설정된 기본 청크 사이즈를 사용하여 파이프라인 전체 실행
     */
    public int runPipeline() {
        return runPipeline(localizationProperties.chunkSize().keyword(), false);
    }

    /**
     * 키워드 한글화 파이프라인 전체 실행
     */
    public int runPipeline(int chunkSize, boolean isSingleRun) {
        log.info("Starting Keyword Localization Pipeline with chunk size {}...", chunkSize);

        extractNewKeywords();

        int processedCount = processUnlocalizedKeywords(chunkSize, isSingleRun);
        // 1개 이상 처리되었을 때만 태그 반영
        if (processedCount > 0) {
            applyLocalizationsToTags();
        }

        log.info("Keyword Localization Pipeline Completed. Processed: {}", processedCount);
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
                log.info("Inserted {} new keywords into Dictionary.", newDictionaries.size());
            }
        });
    }

    /**
     * 환경 변수에 설정된 청크 사이즈를 사용하여 키워드 한글화 실행
     */
    public void processUnlocalizedKeywords() {
        processUnlocalizedKeywords(localizationProperties.chunkSize().keyword(), false);
    }

    /**
     * 지정된 단위(Chunk)로 영문 키워드를 분할하여 한글화 실행
     */
    public int processUnlocalizedKeywords(int chunkSize, boolean isSingleRun) {

        // 데이터 사전에서 한글화되지 않은 영문 키워드 조회
        List<KeywordDictionary> unlocalizedEntities = dictionaryRepository.findUnlocalizedKeywords();
        if (unlocalizedEntities.isEmpty()) {
            log.debug("No unlocalized keywords found. Task skipped.");
            return 0;
        }

        log.info("Found {} unlocalized keywords. Processing in chunks of {}.", unlocalizedEntities.size(), chunkSize);

        int totalProcessed = 0;

        for (int i = 0; i < unlocalizedEntities.size(); i += chunkSize) {
            List<KeywordDictionary> chunk = unlocalizedEntities.subList(i, Math.min(unlocalizedEntities.size(), i + chunkSize));
            processLocalizationChunk(chunk);

            totalProcessed += chunk.size();

            if (isSingleRun) {
                break;
            }

            // API Rate Limit 방어를 위한 청크 간 딜레이
            if (i + chunkSize < unlocalizedEntities.size()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Keyword Localization Chunk processing interrupted", e);
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
        BulkKeywordLocalizationResponse response = sendToAiEngine(request);

        // 결과 DB 반영 (트랜잭션 적용 구간)
        transactionTemplate.executeWithoutResult(status -> {
            applyLocalizationResults(chunkEntities, response);
            // 트랜잭션 외부에서 로드된 엔티티이므로 명시적 업데이트 필수
            dictionaryRepository.saveAll(chunkEntities);
        });
    }

    /**
     * 한글화가 완료된 키워드 사전을 가지고 Tag의 한글 키워드 배열(keywordsKo) 업데이트
     */
    private void applyLocalizationsToTags() {
        transactionTemplate.executeWithoutResult(status -> {
            Map<String, String> dictionaryMap = dictionaryRepository.findAll().stream()
                    .filter(dict -> dict.getKorName() != null)
                    .collect(Collectors.toMap(
                            KeywordDictionary::getEngName,
                            KeywordDictionary::getKorName
                    ));

            if (dictionaryMap.isEmpty()) return;

            List<Tag> targetTags = tagRepository.findTagsNeedingKeywordLocalization();
            int successCount = 0;

            for (Tag tag : targetTags) {
                List<String> localizedKeywords = tag.getKeywords().stream()
                        .map(eng -> dictionaryMap.getOrDefault(eng, eng))
                        .toList();

                tag.updateKeywordsKo(localizedKeywords);
                successCount++;
            }
            log.info("Successfully applied translated keywords to {} tags.", successCount);
        });
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
        return new KeywordLocalizationRequest(keywords);
    }

    /**
     * 한글화 엔진으로 HTTP 요청 전송 및 결과 반환
     */
    private BulkKeywordLocalizationResponse sendToAiEngine(KeywordLocalizationRequest request) {
        String targetUri = localizationProperties.baseUrl() + "/api/localization/keywords";

        try {
            return localizationRestClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BulkKeywordLocalizationResponse.class);
        } catch (Exception e) {
            log.error("Failed to communicate with Keyword Localization Engine. Error: {}", e.getMessage());
            throw new CustomException(ErrorCode.LOCALIZATION_ENGINE_COMMUNICATION_FAILED);
        }
    }

    /**
     * 키워드 한글화 엔진 응답을 원본 사전 엔티티에 매핑
     */
    private void applyLocalizationResults(List<KeywordDictionary> chunkEntities, BulkKeywordLocalizationResponse response) {
        if (response == null || response.localizationResults().isEmpty()) return;

        Map<String, String> responseMap = response.localizationResults().stream()
                .collect(Collectors.toMap(
                        item -> item.engName(),
                        item -> item.korName() != null ? item.korName() : item.engName(),
                        (existing, replacement) -> existing
                ));

        int successCount = 0;
        for (KeywordDictionary entity : chunkEntities) {
            if (responseMap.containsKey(entity.getEngName())) {
                // 번역 누락 또는 FAILED 시 영문 원본 보존
                entity.updateLocalization(responseMap.get(entity.getEngName()));
                successCount++;
            }
        }
        log.info("Successfully localized {} keywords in current chunk.", successCount);
    }
}