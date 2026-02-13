package io.github.seoleeder.owls_pick.client.ITAD;

import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ITADDataCollector {
    private final ITADClient itadClient;

    private static final int PRICE_BATCH_SIZE = 200;

    /**
     * Steam ID 리스트를 가지고 ITAD ID를 받아와서 각각을 매핑
     * @param steamIds ITAD 상의 ID를 받아올 스팀 ID 모음
     * */
    public Map<String, String> collectItadIds(List<String> steamIds){
        if (steamIds == null || steamIds.isEmpty()) return Map.of();

        Map<String, String> resultMap = new HashMap<>();

        for (String steamId : steamIds) {
            try {
                //단일 조회
                String itadUuid = itadClient.findItadIdBySteamId(steamId);

                if (itadUuid != null) {
                    resultMap.put(steamId, itadUuid);
                }
            } catch (Exception e) {
                // id 변환에 실패해도 계속 루프 진행
                log.warn("Failed to collect UUID for SteamID: {}", steamId);
            }
        }

        return resultMap;
    }

    /**
     * DB에 저장된 ITAD ID를 대량으로 받아서 가격 정보 수집 (단일 요청 200개 제한)
     * limit 초과시 배치 작업 수행
     * @param itadIds 가격 정보를 가져올 ITAD ID 리스트
     * */
    public List<ITADPriceResponse> collectPrices(List<String> itadIds){
        if (itadIds == null || itadIds.isEmpty()) return List.of();

        List<ITADPriceResponse> totalResponses = new ArrayList<>();

        // Batching Logic: 200개씩 잘라서 요청
        for (int i = 0; i < itadIds.size(); i += PRICE_BATCH_SIZE) {
            int end = Math.min(i + PRICE_BATCH_SIZE, itadIds.size());
            List<String> batchIds = itadIds.subList(i, end);

            try {
                List<ITADPriceResponse> batchResult = itadClient.getPrices(batchIds);

                if (batchResult != null && !batchResult.isEmpty()) {
                    totalResponses.addAll(batchResult);
                }
            } catch (Exception e) {
                log.error("Failed to fetch prices for batch {}-{}", i, end, e);
            }
        }

        return totalResponses;
    }
}
