package io.github.seoleeder.owls_pick.client.ITAD;

import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADIdResponse;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse;
import io.github.seoleeder.owls_pick.common.config.properties.ItadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class ITADClient {

    private final RestClient restClient;
    private final ItadProperties props;

    public ITADClient(RestClient restClient, ItadProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /**
     * Steam App Id를 ITAD ID로 변환
     * */
    public String findItadIdBySteamId(String steamId){
        if (steamId == null || steamId.isBlank()) return null;

        ITADIdResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("games/lookup/v1")
                        .queryParam("key", props.key())
                        .queryParam("appid", steamId)
                        .build())
                .retrieve()
                .body(ITADIdResponse.class);

        if (response != null && response.found() && response.game() != null) {
            return response.game().id(); //ITAD ID 반환
        }

        return null;
    }

    /**
     * ITAD ID 리스트로 해당 게임들의 스토어 별 가격 데이터 수집
     * @param itadIds 요청을 보낼 ITAD ID 리스트
     * */
    public List<ITADPriceResponse> getPrices (List<String> itadIds){
        if (itadIds.isEmpty()) return List.of();

        ITADPriceResponse[] responses = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("/games/prices/v3")
                        .queryParam("key", props.key())
                        .queryParam("country", "KR")
                        .queryParam("shops", ITADStore.ALL_STORE_IDS)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(itadIds)
                .retrieve()
                .body(ITADPriceResponse[].class);

        return responses != null ? Arrays.asList(responses) : List.of();
    }

}
