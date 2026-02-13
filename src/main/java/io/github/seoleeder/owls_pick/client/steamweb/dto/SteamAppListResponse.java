package io.github.seoleeder.owls_pick.client.steamweb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SteamAppListResponse(Response response) {
    public record Response(
            List<App> apps,
            @JsonProperty("have_more_results") boolean haveMoreResults,
            @JsonProperty("last_appid") Long lastAppId
    ) {
        public record App(
                @JsonProperty("appid") Long appId,
                String name
        ) {}
    }
}
