package io.github.seoleeder.owls_pick.client.IGDB;

import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameDetailResponse;
import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameSummaryResponse;
import io.github.seoleeder.owls_pick.client.IGDB.util.IGDBQueryBuilder;
import io.github.seoleeder.owls_pick.common.config.properties.IgdbProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IGDBClient {
    private final RestClient restClient;
    private final IGDBAuthManager authManager;
    private final IgdbProperties props;

    public IGDBClient(RestClient restClient, IGDBAuthManager authManager, IgdbProperties props) {
        this.restClient = restClient;
        this.authManager = authManager;
        this.props = props;
    }

    /**IGDB의 모든 메인 게임 데이터 수집
     * 초기 대량 수집 로직
     * Steam에 등록된 모든 게임들만 수집 (DLC, 확장팩 제외)
     * 출시 상태, 지원 플랫폼, 심의, 출시일, 게임 타입 등
     * IGDBQueryBuilder를 통해 체이닝 방식으로 RequestBody 생성
     * @param lastId 마지막으로 수집한 게임의 id. 중복 및 누락 없이 순차적 수집 가능
     * */
    public List<IGDBGameSummaryResponse> getGameSummaryList(Long lastId, int limit) {

        String requestBody = IGDBQueryBuilder.create()
                .fields(
                        "external_games.external_game_source",
                        "age_ratings.rating_category.rating",
                        "game_type.type",
                        "cover.image_id",
                        "first_release_date",
                        "game_localizations.name",
                        "game_localizations.region.name",
                        "game_modes.name",
                        "game_status.status",
                        "platforms.name",
                        "player_perspectives.name",
                        "storyline",
                        "summary",
                        "updated_at",
                        "hypes"
                )
                .where("external_games.external_game_source = 1")
                .where("first_release_date != null")
                .where("genres != null")
                .where("themes != null")
                .where("summary != null")
                .where("game_type.type != null")
                .where("cover.image_id != null")
                .where("game_status.status != null")
                .where("id > " + lastId)
                .sort("id asc")
                .limit(limit) // default : 10, max : 500
                .build();

        return sendRequest(requestBody, IGDBGameSummaryResponse[].class);
    }

    /**IGDB의 모든 메인 게임 데이터 수집
     * 업데이트된 데이터 + 초기 수집 이후에 추가된 데이터 수집 로직
     * Steam에 등록된 모든 게임들만 수집 (DLC, 확장팩 제외)
     * 출시 상태, 지원 플랫폼, 심의, 출시일, 게임 타입 등
     * IGDBQueryBuilder를 통해 체이닝 방식으로 RequestBody 생성
     * @param lastTimestamp 마지막으로 수집한 시점
     * */
    public List<IGDBGameSummaryResponse> getUpdatedGameSummaryList(Long lastTimestamp, int limit) {

        String requestBody = IGDBQueryBuilder.create()
                .fields(
                        "external_games.external_game_source",
                        "age_ratings.organization.name",
                        "age_ratings.rating_category.rating",
                        "game_type.type",
                        "cover.image_id",
                        "first_release_date",
                        "game_localizations.name",
                        "game_localizations.region.name",
                        "game_modes.name",
                        "game_status.status",
                        "platforms.name",
                        "player_perspectives.name",
                        "storyline",
                        "summary",
                        "updated_at",
                        "hypes"
                )
                .where("external_games.external_game_source = 1")
                .where("first_release_date != null")
                .where("genres != null")
                .where("themes != null")
                .where("summary != null")
                .where("game_type.type != null")
                .where("cover.image_id != null")
                .where("game_status.status != null")
                .where("updated_at > " + lastTimestamp)
                .sort("updated_at asc")
                .limit(limit) // default : 10, max : 500
                .build();

        return sendRequest(requestBody, IGDBGameSummaryResponse[].class);
    }

    /** getGameSummary를 통해 가져온 IGDB ID를 통해 게임 상세정보 대량 요청
     * @param gameIds DB에 저장된 IGDB_ID 리스트
     * */
    public List<IGDBGameDetailResponse> getGameDetailList(List<Long> gameIds, Integer limit) {
        if (gameIds.isEmpty()) return List.of();

        String idString = gameIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String requestBody = IGDBQueryBuilder.create()
                .fields(

                        //매핑용
                        "external_games.external_game_source",

                        //game_tag 데이터
                        "genres.name",
                        "themes.name",
                        "keywords.name",

                        //게임 개발 및 출시에 관여한 회사 데이터
                        "involved_companies.developer",
                        "involved_companies.publisher",
                        "involved_companies.company.name",
                        "involved_companies.company.websites.type",
                        "involved_companies.company.websites.url",
                        "involved_companies.company.logo.image_id",

                        //스크린샷 데이터
                        "screenshots.image_id",
                        "screenshots.width",
                        "screenshots.height",

                        //언어 지원 여부 데이터
                        "language_supports.language.name",
                        "language_supports.language_support_type.name"


                )
                .where("external_games.external_game_source = 1")
                .where("genres != null")
                .where("themes != null")
                .where("summary != null")
                .where("game_type.type != null")
                .where("cover.image_id != null")
                .where("game_status.status != null")
                .where("id = (" + idString + ")")
                .sort("id asc")
                .limit(limit)
                .build();

        return sendRequest(requestBody, IGDBGameDetailResponse[].class);
    }



    private <T> List<T> sendRequest(String body, Class<T[]> responseType) {
        T[] responses = restClient.post()
                .uri( uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.baseUrl())
                        .path("games")
                        .build())
                .header("Client-ID", props.clientId())
                .header("Authorization", "Bearer " + authManager.getAccessToken())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body)
                .retrieve()
                .body(responseType);

        return responses != null ? Arrays.asList(responses) : List.of();
    }

}
