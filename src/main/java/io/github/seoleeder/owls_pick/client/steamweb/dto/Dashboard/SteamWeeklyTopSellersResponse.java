package io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SteamWeeklyTopSellersResponse(
        Response response) {

    public record Response(
            @JsonProperty("start_date") Long startDate,
            @JsonProperty("ranks") List<Rank> ranks
    ){
        public record Rank(
                @JsonProperty("rank") int rank,
                @JsonProperty("appid") Long appid
        ){
        }
    }
}
