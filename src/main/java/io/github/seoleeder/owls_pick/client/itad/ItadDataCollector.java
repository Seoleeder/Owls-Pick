package io.github.seoleeder.owls_pick.client.itad;

import io.github.seoleeder.owls_pick.client.itad.dto.ItadBulkResponse;
import io.github.seoleeder.owls_pick.client.itad.dto.ItadPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItadDataCollector {
    private final ItadApiCaller itadApiCaller;

    /**
     * Steam ID를 가지고 ITAD ID를 받아와서 매핑
     * @param steamId ITAD ID를 받아올 스팀 ID
     * */
    public String collectItadId(String steamId){
        if (steamId == null || steamId.isBlank()) return null;

        try {
            // Caller를 통한 안전한 단일 조회
            return itadApiCaller.findItadIdSafe(steamId);
        } catch (Exception e) {
            log.warn("Failed to collect UUID for SteamID: {} - {}", steamId, e.getMessage());
            return null;
        }
    }

    /**
     * DB에 저장된 스팀 ID를 대량으로 받아서 ITAD ID를 한 번에 매핑
     * @param shopId 상점 고유 ID
     * @param formattedSteamIds 포맷팅된 스팀 ID 리스트
     */
    public ItadBulkResponse collectItadIdsBulk(int shopId, List<String> formattedSteamIds) {
        if (formattedSteamIds == null || formattedSteamIds.isEmpty()) {
            return new ItadBulkResponse(new HashMap<>());
        }

        try {
            return itadApiCaller.findItadIdsBulkSafe(shopId, formattedSteamIds);
        } catch (Exception e) {
            log.warn("Failed to collect UUIDs in bulk. batch size: {} - {}", formattedSteamIds.size(), e.getMessage());
            return new ItadBulkResponse(new HashMap<>());
        }
    }

    /**
     * DB에 저장된 ITAD ID를 대량으로 받아서 가격 정보 수집
     * @param itadIds 가격 정보를 가져올 ITAD ID 리스트
     * */
    public List<ItadPriceResponse> collectPrices(List<String> itadIds){
        if (itadIds == null || itadIds.isEmpty()) return List.of();
        try {
            // Caller를 통한 안전한 가격 정보 수집
            List<ItadPriceResponse> response = itadApiCaller.getPricesSafe(itadIds);

            return response != null ? response : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch prices for batch size: {} - {}", itadIds.size(), e.getMessage());
            return List.of();
        }
    }
}
