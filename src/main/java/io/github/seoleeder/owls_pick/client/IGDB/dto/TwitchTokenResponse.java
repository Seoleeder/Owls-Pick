package io.github.seoleeder.owls_pick.client.IGDB.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TwitchTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("tokenType") String tokenType
) {
}
