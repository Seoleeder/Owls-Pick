package io.github.seoleeder.owls_pick.dto.auth;

import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String nickname,
        String email
) {
}
