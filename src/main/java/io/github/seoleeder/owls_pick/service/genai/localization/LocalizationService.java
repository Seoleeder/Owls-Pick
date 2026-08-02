package io.github.seoleeder.owls_pick.service.genai.localization;

import io.github.seoleeder.owls_pick.dto.request.LocalizationBulkRequest;
import io.github.seoleeder.owls_pick.dto.response.LocalizationBulkResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.genai.GenaiFailedTask;
import io.github.seoleeder.owls_pick.entity.genai.enums.GenaiFailReason;
import io.github.seoleeder.owls_pick.entity.genai.enums.GenaiPipelineType;
import io.github.seoleeder.owls_pick.global.config.properties.GenaiProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.GenaiFailedTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalizationService {

    private final GameRepository gameRepository;
    private final RestClient localizationRestClient;
    private final GenaiProperties props;
    private final TransactionTemplate transactionTemplate;
    private final GenaiFailedTaskRepository failedTaskRepository;

    public LocalizationService(
            GameRepository gameRepository,
            @Qualifier("genaiRestClient") RestClient localizationRestClient,
            GenaiProperties props,
            TransactionTemplate transactionTemplate,
            GenaiFailedTaskRepository failedTaskRepository) {

        this.gameRepository = gameRepository;
        this.localizationRestClient = localizationRestClient;
        this.props = props;
        this.transactionTemplate = transactionTemplate;
        this.failedTaskRepository = failedTaskRepository;
    }

    // 비동기 콜백 상태 관리를 위한 인메모리 맵
    private final Map<String, CompletableFuture<LocalizationBulkResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 환경 변수에 설정된 기본 청크 사이즈로 한글화 파이프라인 실행
     */
    public void runPipeline() {
        runPipeline(props.localization().game().chunkSize());
    }

    /**
     * 지정된 청크 단위로 한글화 파이프라인 연속 실행
     */
    public void runPipeline(int chunkSize) {
        log.info("[GenAI] Starting Game Description Localization Pipeline with chunk size {}...", chunkSize);
        int totalProcessed = 0;

        while (true) {
            try {

                int processedCount = processLocalizationChunk(chunkSize);

                if (processedCount == 0) {
                    break; // 한글화되지 않은 데이터 소진 시 루프 탈출
                }
                totalProcessed += processedCount;

            } catch (Exception e) {
                // 단일 청크 처리 실패 시 로그 기록 후 다음 주기로 이동
                log.error("[GenAI] Failed to process localization chunk. Skipping to next cycle.", e);
            }

            try {
                // API 부하 방지를 위한 대기 시간 적용
                Thread.sleep(props.localization().delayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[GenAI] Game Description Localization Pipeline sleep interrupted", e);
                break;
            }
        }
        log.info("[GenAI] Game Description Localization Pipeline Finished. Total localized games: {}", totalProcessed);
    }

    /**
     * 지정된 단위(Chunk)로 미번역 게임 데이터를 조회하여 한글화 파이프라인 실행
     */
    public int processLocalizationChunk(int chunkSize) {
        List<Game> targetGames = gameRepository.findUnlocalizedGames(chunkSize);
        if (targetGames.isEmpty()) {
            log.debug("[GenAI] No unlocalized games found. Task skipped.");
            return 0;
        }

        try {
            // 요청 DTO 조립
            LocalizationBulkRequest request = buildRequestDto(targetGames);

            // 한글화 엔진 통신
            LocalizationBulkResponse response = sendToAiEngine(request);

            // 결과 DB 반영
            Integer result = transactionTemplate.execute(status -> {

                // 영속 상태의 엔티티 일괄 재조회
                List<Long> gameIds = targetGames.stream().map(Game::getId).toList();
                List<Game> managedGames = gameRepository.findAllById(gameIds);

                // 한글화 결과 매핑 및 상태 업데이트
                return applyLocalizationResults(managedGames, response);
            });

            return result != null ? result : 0;
        } catch (Exception e) {
            // 통신 장애 시 대상 청크를 실패 작업으로 기록
            log.error("[GenAI] Failed to process localization chunk. Recording {} games to DLQ.", targetGames.size(), e);
            Map<Long, GenaiFailReason> failedTaskMap = targetGames.stream()
                    .collect(Collectors.toMap(Game::getId, game -> GenaiFailReason.NETWORK_ERROR));
            recordFailedTasks(failedTaskMap);

            // 루프 유지를 위해 현재 청크 사이즈 반환
            return targetGames.size();
        }
    }

    /**
     * 게임 데이터 한글화 실패 작업 재시도
     */
    public void retryFailedTasks() {
        // 아직 조치되지 않은 한글화 실패 내역 조회
        List<GenaiFailedTask> failedTasks = failedTaskRepository.findUnhandledTasks(GenaiPipelineType.GAME_LOCALIZATION);
        if (failedTasks.isEmpty()) {
            return;
        }

        log.info("[GenAI] Retrying {} failed Game Localization tasks...", failedTasks.size());

        // 사전에 정의된 청크 사이즈 할당
        int chunkSize = props.localization().game().chunkSize();

        // 실패 작업 API 한도 방어를 위한 청크 단위 분할 처리
        for (int i = 0; i < failedTasks.size(); i += chunkSize) {
            List<GenaiFailedTask> taskChunk = failedTasks.subList(i, Math.min(failedTasks.size(), i + chunkSize));
            List<Long> gameIds = taskChunk.stream().map(GenaiFailedTask::getTargetId).toList();

            try {
                // 실패 대상 게임 엔티티 조회
                List<Game> targetGames = gameRepository.findAllById(gameIds);

                // 통신 DTO 생성 및 한글화 재요청
                LocalizationBulkRequest request = buildRequestDto(targetGames);
                LocalizationBulkResponse response = sendToAiEngine(request);

                // 재시도 결과 적용 및 실패 작업 조치 완료 처리
                transactionTemplate.executeWithoutResult(status -> {
                    // 원본 게임 엔티티 영속화 및 한글화 텍스트 반영
                    List<Game> managedGames = gameRepository.findAllById(gameIds);
                    applyLocalizationResults(managedGames, response);

                    // 실패 이력 영속화 및 처리 상태(isHandled) 갱신
                    List<Long> taskIds = taskChunk.stream().map(GenaiFailedTask::getId).toList();
                    List<GenaiFailedTask> managedTasks = failedTaskRepository.findAllById(taskIds);
                    managedTasks.forEach(GenaiFailedTask::markAsHandled);
                });
            } catch (Exception e) {
                // 재시도 단일 청크 실패 시 로그 기록 후 흐름 유지
                log.error("[GenAI] Failed to retry Game Localization chunk. Skipping to next chunk.", e);
            }
        }
    }

    /**
     * Webhook 수신 시 대기 중인 요청 식별자를 찾아 결과 반환 및 스레드 대기 해제
     */
    public void completePendingTask(LocalizationBulkResponse response) {
        CompletableFuture<LocalizationBulkResponse> future = pendingRequests.get(response.requestId());
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
     * Game 엔티티 리스트를 외부 한글화 엔진 통신용 Request DTO로 변환
     */
    private LocalizationBulkRequest buildRequestDto(List<Game> games) {
        List<LocalizationBulkRequest.GameItem> items = games.stream()
                .map(game -> new LocalizationBulkRequest.GameItem(
                        game.getId(),
                        game.getDescription(),
                        game.getStoryline()
                )).toList();

        return new LocalizationBulkRequest(null, null, items);
    }

    /**
     * 한글화 엔진으로 실제 HTTP 요청을 보내고 한글화 결과 반환
     */
    private LocalizationBulkResponse sendToAiEngine(LocalizationBulkRequest request) {

        // 요청 및 콜백 매핑용 고유 식별자 발급
        String requestId = UUID.randomUUID().toString();

        // Base URL과 엔드포인트를 조합하여 콜백 URL 생성
        String callbackUrl = props.callbackBaseUrl() + "/api/internal/callback/genai/localization";

        LocalizationBulkRequest asyncRequest = new LocalizationBulkRequest(
                requestId, callbackUrl, request.games());

        log.info("[GenAI] Sending bulk localization request for {} games to AI Engine...", request.games().size());

        URI targetUri = UriComponentsBuilder.fromUriString(props.fastapiUrl())
                .path("/api/localization/games/bulk")
                .build()
                .toUri();

        try {
            // 한글화 요청 전송 직후 커넥션 해제
            localizationRestClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(asyncRequest)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("[GenAI] Communication Error with GenAI Server for Game Localization");
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }

        // 인메모리 스레드 대기 상태 전환
        CompletableFuture<LocalizationBulkResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {

            // Webhook 응답 수신까지 스레드 대기
            return future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("[GenAI] Webhook callback timeout for Request ID: {}", requestId);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        } catch (Exception e) {
            log.error("[GenAI] Failed to wait for webhook callback for Request ID: {}", requestId, e);
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        } finally {
            // 처리 완료된 요청 식별자 제거 (메모리 누수 방지)
            pendingRequests.remove(requestId);
        }
    }

    /**
     * 한글화 엔진으로부터 반환된 결과를 원본 게임 엔티티에 매핑 후 업데이트
     */
    private int applyLocalizationResults(List<Game> targetGames, LocalizationBulkResponse response) {
        // 빠른 조회를 위해 List를 Map으로 변환
        Map<Long, Game> gameMap = targetGames.stream()
                .collect(Collectors.toMap(Game::getId, g -> g));

        int successCount = 0;

        Map<Long, GenaiFailReason> partialFailures = new HashMap<>();

        for (LocalizationBulkResponse.ResultItem result : response.results()) {
            Game game = gameMap.get(result.gameId());

            if (game == null) {
                continue;
            }

            // 개별 게임 한글화 실패 사유(errorReason) 존재 시, 매핑 생략 및 실패 작업 적재
            if (result.errorReason() != null) {
                GenaiFailReason failReason = parseFailReason(result.errorReason());
                log.warn("[GenAI] AI returned failed status for Game ID: {}. Reason: {}. Recording to FailedTask.", game.getId(), failReason);
                partialFailures.put(game.getId(), failReason);
                continue;
            }

            game.updateLocalization(result.descriptionKo(), result.storylineKo());
            successCount++;
        }

        // 청크 내 부분 실패 건이 존재하는 경우 1번의 배치 인서트로 일괄 적재
        if (!partialFailures.isEmpty()) {
            recordFailedTasks(partialFailures);
            log.warn("[GenAI] Recorded {} partial failure tasks to DLQ.", partialFailures.size());
        }

        log.info("[GenAI] Successfully updated {} localized games in Database.", successCount);
        return successCount;
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
            log.warn("[GenAI] Unknown FailReason received: {}. Falling back to UNKNOWN_ERROR.", reasonStr);
            return GenaiFailReason.UNKNOWN_ERROR;
        }
    }

    /**
     * 추후 재시도 및 통계를 위한 실패 대상 식별자 및 사유 목록 일괄 적재
     */
    private void recordFailedTasks(Map<Long, GenaiFailReason> failedTaskMap) {
        if (failedTaskMap.isEmpty()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            List<GenaiFailedTask> failedTasks = failedTaskMap.entrySet().stream()
                    .map(entry -> GenaiFailedTask.builder()
                            .pipelineType(GenaiPipelineType.GAME_LOCALIZATION)
                            .targetId(entry.getKey())
                            .failReason(entry.getValue())
                            .build())
                    .toList();

            failedTaskRepository.saveAll(failedTasks);
        });
    }
}
