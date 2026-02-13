package io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SteamDashboardRequest(
        @JsonProperty("country_code") String countryCode,
        Context context,
        @JsonProperty("start_date") Long startDate,
        @JsonProperty("page_start") Integer pageStart,
        @JsonProperty("page_count") Integer pageCount
        ) {
    public record Context(
            @JsonProperty("country_code") String countryCode
    ){}
}

