package io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SteamMostPlayedAppResponse(
        @JsonProperty("rollup_date") Long updatedAt,
        @JsonProperty("ranks") List<Rank> ranks
) {
    public record Rank (
            @JsonProperty("rank") int rank,
            @JsonProperty("appid") Long appid
    ){
    }
}
