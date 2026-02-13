package io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SteamYearOrMonthTopAppResponse(
        Response response
) {

    public record Response(
            @JsonProperty("top_combined_app_and_dlc_releases") List<TopApp> topApp
    ) {
        public record TopApp(
                @JsonProperty("appid") Long appId,
                @JsonProperty("app_release_rank") int rank
        ){
        }
    }

}
