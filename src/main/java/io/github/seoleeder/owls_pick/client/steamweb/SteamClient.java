package io.github.seoleeder.owls_pick.client.steamweb;

import io.github.seoleeder.owls_pick.client.QueryParamSerializer;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamConcurrentPlayersTopAppResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamMostPlayedAppResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamWeeklyTopSellersResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamYearOrMonthTopAppResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.request.SteamDashboardRequest;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.request.SteamDashboardRequest.Context;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewDetailResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewStatsResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.SteamAppListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SteamClient{
    @Value("${external-api.steam.key}")
    private String apiKey;

    @Value("${external-api.steam.base-url.web}")
    private String STEAM_WEB_API_URL;

    @Value("${external-api.steam.base-url.store}")
    private String STEAM_STORE_API_URL;

    private final RestClient restClient;
    private final QueryParamSerializer queryParamSerializer;

    /**
     * Steam에 등록되어 있는 모든 게임 ID, 타이틀 수집 (공개된 "게임"들만)
     * @param lastAppId null이면 처음부터 수집, 값이 존재하면 해당 ID 이후부터 수집
     * @param maxResult 조회 한 번으로 가져올 게임 수 (최대 50k 지원. 안정성을 위해 10k default)
     * */
    public SteamAppListResponse getAppList(Long lastAppId, Integer maxResult){
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .scheme("https")
                            .host(STEAM_WEB_API_URL)
                            .path("IStoreService/GetAppList/v1/")
                            .queryParam("key", apiKey)
                            .queryParam("include_games", true)
                            .queryParam("max_results", maxResult != null ? maxResult : 10000);

                    if (lastAppId != null){
                        uriBuilder.queryParam("last_appid", lastAppId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(SteamAppListResponse.class);
    }

    /**
     * Steam app id로 리뷰 통계 데이터 수집. num_per_page = 20으로 비용 최소화
     * @param appId 조회 대상이 되는 게임의 ID
     * */
    public SteamReviewStatsResponse getReviewStat(Long appId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_STORE_API_URL)
                        .path("/appreviews/{appId}")
                        .queryParam("json", 1)
                        .queryParam("language", "korean")
                        .queryParam("filter", "recent")    // 최신순으로 정렬
                        .queryParam("purchase_type", "all")   // 모든 리뷰 통계 데이터 수집
                        .queryParam("num_per_page", 20)   // 최소 리소스 사용
                        .build())
                .retrieve()
                .body(SteamReviewStatsResponse.class);
    }

    /**
     * Steam app id로 리뷰 상세 데이터 수집, 처리 속도 극대화를 위해 num_per_page = 100으로 설정
     * @param appId 조회 대상이 되는 게임의 ID
     * @param cursor 다음 페이지의 리뷰를 가져오기 위해 필요한 값
     * @param reviewType 긍정/부정 리뷰 설정
     * */
    public SteamReviewDetailResponse getReviewDetail(Long appId, String cursor, String reviewType) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_STORE_API_URL)
                        .path("/appreviews/{appId}")
                        .queryParam("json", 1)
                        .queryParam("language", "korean")
                        .queryParam("filter", "recent")    // 최신순으로 정렬
                        .queryParam("review_type", reviewType) // positive 또는 negative
                        .queryParam("cursor", cursor)
                        .queryParam("num_per_page", 100)   // 처리 속도 극대화
                        .build())
                .retrieve()
                .body(SteamReviewDetailResponse.class);
    }

    /**
     * Steam에서 집계된 주간 최고 인기 게임 데이터 수집
     * 매주 화요일 오전 1시마다 게시됨
     * SteamDashboardRequest 파라미터 주입 -> DTO를 JSON으로 변환 -> 쿼리 파라미터에 넣어서 요청
     * @param countryCode 집계가 반영되는 국가 코드 (대한민국 = KR)
     * @param pageStart 해당 페이지에서 몇위부터 반환할지 명시
     * @param pageCount 페이지당 가져올 게임의 개수 (매출별로 정렬)
     * */
    public SteamWeeklyTopSellersResponse getWeeklyTopSeller(String countryCode, Long startDate, Integer pageStart, Integer pageCount){

        //requestDTO에 요청 파라미터 주입
        Context context = new SteamDashboardRequest.Context(countryCode);
        SteamDashboardRequest requestDto = new SteamDashboardRequest(countryCode, context, startDate, pageStart, pageCount);

        // DTO -> JSON 변환
        String jsonParam = queryParamSerializer.serialize(requestDto);

        return restClient.get()
                .uri( uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_WEB_API_URL)
                        .path("IStoreTopSellersService/GetWeeklyTopSellers/v1")
                        .queryParam("key", apiKey)
                        .queryParam("input_json", jsonParam)
                        .build())
                .retrieve()
                .body(SteamWeeklyTopSellersResponse.class);
    }

    /**
     * Steam에서 집계된 월간 최고 인기 게임 데이터 수집
     * 매월 15일 오전 10시마다 게시됨 (전월 데이터)
     * @param rtimeMonth 기준이 되는 timestamp
     * */
    public SteamYearOrMonthTopAppResponse getMonthTopApp(Long rtimeMonth) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_STORE_API_URL)
                        .path("ISteamChartsService/GetMonthTopAppReleases/v1")
                        .queryParam("key", apiKey)
                        .queryParam("rtime_month", rtimeMonth)
                        .build())
                .retrieve()
                .body(SteamYearOrMonthTopAppResponse.class);
    }

    /**
     * Steam에서 집계된 월간 최고 인기 게임 데이터 수집
     * 매월 15일 오전 10시마다 게시됨 (전월 데이터)
     * @param rtimeYear 기준이 되는 timestamp
     * */
    public SteamYearOrMonthTopAppResponse getYearTopApp(Long rtimeYear) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_STORE_API_URL)
                        .path("ISteamChartsService/GetYearTopAppReleases/v1")
                        .queryParam("key", apiKey)
                        .queryParam("rtime_year", rtimeYear)
                        .build())
                .retrieve()
                .body(SteamYearOrMonthTopAppResponse.class);
    }

    /**
     * Steam에서 동시 접속자수가 가장 많은 게임 데이터 수집
     * (현재 플레이어 수 기준, 15분마다 집계)
     * @param countryCode 집계가 반영되는 국가 코드 (대한민국 = KR)
     * */
    public SteamConcurrentPlayersTopAppResponse getConcurrentPlayersTopApp(String countryCode){

        //requestDTO에 요청 파라미터 주입
        Context requestDto = new SteamDashboardRequest.Context(countryCode);

        // DTO -> JSON 변환
        String jsonParam = queryParamSerializer.serialize(requestDto);

        return restClient.get()
                .uri( uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_WEB_API_URL)
                        .path("ISteamChartsService/GetGamesByConcurrentPlayers/v1")
                        .queryParam("key", apiKey)
                        .queryParam("input_json", jsonParam)
                        .build())
                .retrieve()
                .body(SteamConcurrentPlayersTopAppResponse.class);
    }

    /**
     * Steam에서 가장 많이 플레이된 게임 데이터 수집
     * (일일 플레이어 수 기준, 24시간마다 집계)
     * @param countryCode 집계가 반영되는 국가 코드 (대한민국 = KR)
     * */
    public SteamMostPlayedAppResponse getMostPlayedApp(String countryCode){

        //requestDTO에 요청 파라미터 주입
        Context requestDto = new SteamDashboardRequest.Context(countryCode);

        // DTO -> JSON 변환
        String jsonParam = queryParamSerializer.serialize(requestDto);

        return restClient.get()
                .uri( uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(STEAM_WEB_API_URL)
                        .path("ISteamChartsService/GetMostPlayedGames/v1/")
                        .queryParam("key", apiKey)
                        .queryParam("input_json", jsonParam)
                        .build())
                .retrieve()
                .body(SteamMostPlayedAppResponse.class);
    }
}
