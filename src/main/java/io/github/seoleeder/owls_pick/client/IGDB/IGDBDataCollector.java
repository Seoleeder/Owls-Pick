package io.github.seoleeder.owls_pick.client.IGDB;

import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameDetailResponse;
import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IGDBDataCollector {

    private final IGDBClient igdbClient;

    private static final Integer IGDB_LIMIT = 500;

    /**
     * 초기 대량 데이터 수집
     * 조건에 맞는 게임 주요 데이터 수집 (Pagination)
     */
    public List<IGDBGameSummaryResponse> collectGameSummary(Long lastId) {
        return igdbClient.getGameSummaryList(lastId, IGDB_LIMIT);
    }

    /**
     * 수정 시간 기준 새로 업데이트된 데이터 수집
     * */
    public List<IGDBGameSummaryResponse> collectUpdatedGameSummary(Long lastTimestamp) {
        return igdbClient.getUpdatedGameSummaryList(lastTimestamp, IGDB_LIMIT);
    }

    /**
     * 수집된 IGDB_ID로 게임 상세 정보 수집
     * IGDB가 제안하는 Limit에 맞게 단일 요청 500으로 제한
     * 초과 요청할 시 500씩 끊어서 요청
     */
    public List<IGDBGameDetailResponse> collectGameDetail(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return List.of();
        }

        List<IGDBGameDetailResponse> totalResults = new ArrayList<>();

        for (int i = 0; i < gameIds.size(); i += IGDB_LIMIT) {
            int end = Math.min(i + IGDB_LIMIT, gameIds.size());
            List<Long> batchIds = gameIds.subList(i, end);

            try {
                List<IGDBGameDetailResponse> batchResponse = igdbClient.getGameDetailList(batchIds, IGDB_LIMIT);
                if (batchResponse != null) {
                    totalResults.addAll(batchResponse);
                }
            } catch (Exception e) {
                log.error("Failed batch {}-{}", i, end, e);
            }
        }
        return totalResults;
    }
}
