package io.github.seoleeder.owls_pick.client.itad;

import io.github.seoleeder.owls_pick.client.itad.dto.ItadBulkResponse;
import io.github.seoleeder.owls_pick.client.itad.dto.ItadIdResponse;
import io.github.seoleeder.owls_pick.client.itad.dto.ItadPriceResponse;
import io.github.seoleeder.owls_pick.global.config.properties.ItadProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
public class ItadClient {

    private final RestClient restClient;
    private final ItadProperties props;

    public ItadClient(RestClient restClient, ItadProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /**
     * Steam App Id를 ITAD ID로 변환
     * */
    public String findItadIdBySteamId(String steamId){
        if (steamId == null || steamId.isBlank()) return null;

        ItadIdResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("games/lookup/v1")
                        .queryParam("key", props.key())
                        .queryParam("appid", steamId)
                        .build())
                .retrieve()
                .body(ItadIdResponse.class);

        if (response != null && response.found() && response.game() != null) {
            return response.game().id(); //ITAD ID 반환
        }

        return null;
    }

    /**
     * Steam App Id 리스트를 ITAD ID 응답 DTO로 한 번에 다건 변환
     * @param shopId 상점 고유 ID (Steam = 61)
     * @param shopGameIds ITAD 포맷("app/123")으로 변환된 스팀 ID 리스트
     */
    public ItadBulkResponse findItadIdsBulk(int shopId, List<String> shopGameIds) {
        if (shopGameIds == null || shopGameIds.isEmpty()) {
            return new ItadBulkResponse(new HashMap<>());
        }

        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("/lookup/id/shop/{shopId}/v1")
                        .queryParam("key", props.key())
                        .build(shopId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(shopGameIds)
                .retrieve()
                .body(ItadBulkResponse.class);
    }

    /**
     * ITAD ID 리스트로 해당 게임들의 스토어 별 가격 데이터 수집
     * @param itadIds 요청을 보낼 ITAD ID 리스트
     * */
    public List<ItadPriceResponse> getPrices (List<String> itadIds){
        if (itadIds.isEmpty()) return List.of();

        ItadPriceResponse[] responses = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("/games/prices/v3")
                        .queryParam("key", props.key())
                        .queryParam("country", "KR")
                        .queryParam("shops", ItadStore.ALL_STORE_IDS)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(itadIds)
                .retrieve()
                .body(ItadPriceResponse[].class);

        return responses != null ? Arrays.asList(responses) : List.of();
    }

}
