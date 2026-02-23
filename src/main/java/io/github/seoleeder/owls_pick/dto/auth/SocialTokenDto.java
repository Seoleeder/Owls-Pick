package io.github.seoleeder.owls_pick.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SocialTokenDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token")     String idToken // 카카오, 구글은 값 존재 / 네이버는 null
) {}
