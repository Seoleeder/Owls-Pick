package io.github.seoleeder.owls_pick.service.localization;

import io.github.seoleeder.owls_pick.dto.request.BulkLocalizationRequest;
import io.github.seoleeder.owls_pick.dto.response.LocalizationBulkResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.global.config.properties.GenaiProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalizationService {

    private final GameRepository gameRepository;
    private final RestClient localizationRestClient;
    private final GenaiProperties genaiProperties;
    private final TransactionTemplate transactionTemplate;


    public LocalizationService(
            GameRepository gameRepository,
            @Qualifier("genaiRestClient") RestClient localizationRestClient,
            GenaiProperties genaiProperties,
            TransactionTemplate transactionTemplate) {

        this.gameRepository = gameRepository;
        this.localizationRestClient = localizationRestClient;
        this.genaiProperties = genaiProperties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 환경 변수에 설정된 기본 청크 사이즈를 사용하여 한글화 파이프라인 전체 실행
     */
    public void runPipeline() {
        runPipeline(genaiProperties.localization().chunkSize().game());
    }

    /**
     * 지정된 단위(Chunk)를 매개로 받아 한글화 파이프라인 무한 루프 실행
     */
    public void runPipeline(int chunkSize) {
        log.info("Starting Game Description Localization Pipeline with chunk size {}...", chunkSize);
        int totalProcessed = 0;

        while (true) {
            // 전달받은 chunkSize를 사용하여 단일 청크 처리
            int processedCount = processLocalizationChunk(chunkSize);

            if (processedCount == 0) {
                break; // 미번역 데이터 소진 시 루프 탈출
            }
            totalProcessed += processedCount;

            try {
                // API 부하 방지를 위한 2초 딜레이
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Game Description Localization Pipeline sleep interrupted", e);
                break;
            }
        }
        log.info("Game Description Localization Pipeline Finished. Total localized games: {}", totalProcessed);
    }

    /**
     * 지정된 단위(Chunk)로 미번역 게임 데이터를 조회하여 한글화 파이프라인 실행
     */
    public int processLocalizationChunk(int chunkSize) {
        List<Game> targetGames = gameRepository.findUnlocalizedGames(chunkSize);
        if (targetGames.isEmpty()) {
            log.debug("No unlocalized games found. Task skipped.");
            return 0;
        }

        // 요청 DTO 조립
        BulkLocalizationRequest request = buildRequestDto(targetGames);

        // 한글화 엔진 통신
        LocalizationBulkResponse response = sendToAiEngine(request);

        // 결과 DB 반영 (영속성 컨텍스트 더티 체킹)
        Integer result = transactionTemplate.execute(status -> {
            int successCount = applyLocalizationResults(targetGames, response);
            gameRepository.saveAll(targetGames);
            return successCount;
        });

        return result != null ? result : 0;
    }

    // ---------------------------------------------------------------------------------
    // Helper Methods
    // ---------------------------------------------------------------------------------

    /**
     * Game 엔티티 리스트를 외부 한글화 엔진 통신용 Request DTO로 변환
     */
    private BulkLocalizationRequest buildRequestDto(List<Game> games) {
        List<BulkLocalizationRequest.GameItem> items = games.stream()
                .map(game -> new BulkLocalizationRequest.GameItem(
                        game.getId(),
                        game.getDescription(),
                        game.getStoryline()
                )).toList();

        return new BulkLocalizationRequest(items);
    }

    /**
     * 한글화 엔진으로 실제 HTTP 요청을 보내고 번역 결과 반환
     */
    private LocalizationBulkResponse sendToAiEngine(BulkLocalizationRequest request) {
        log.info("Sending bulk localization request for {} games to AI Engine...", request.games().size());

        String targetUri = genaiProperties.fastapiUrl() + "/api/localization/bulk";

        LocalizationBulkResponse response;

        try {
            response = localizationRestClient.post()
                    .uri(targetUri) // 완성된 전체 URL 삽입
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LocalizationBulkResponse.class);
        } catch (Exception e) {
            log.error("Failed to communicate with Localization Engine. Error: {}", e.getMessage());
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }

        if (response == null || !response.success() || response.results() == null) {
            log.error("AI Engine returned invalid response or task failed.");
            throw new CustomException(ErrorCode.FASTAPI_COMMUNICATION_FAILED);
        }
        return response;

    }

    /**
     * 한글화 엔진으로부터 반환된 결과를 원본 게임 엔티티에 매핑 후 업데이트
     */
    private int applyLocalizationResults(List<Game> targetGames, LocalizationBulkResponse response) {
        // 빠른 조회를 위해 List를 Map으로 변환 (O(N) 성능 최적화 유지)
        Map<Long, Game> gameMap = targetGames.stream()
                .collect(Collectors.toMap(Game::getId, g -> g));

        int successCount = 0;
        for (LocalizationBulkResponse.ResultItem result : response.results()) {
            Game game = gameMap.get(result.gameId());
            if (game != null) {
                game.updateLocalization(result.descriptionKo(), result.storylineKo());
                successCount++;
            }
        }

        log.info("Successfully updated {} localized games in Database.", successCount);
        return successCount;
    }
}
