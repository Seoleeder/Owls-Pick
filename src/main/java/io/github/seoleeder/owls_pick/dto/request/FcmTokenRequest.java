package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 토큰 등록 요청 DTO")
public record FcmTokenRequest(
        @NotBlank(message = "Token string is required.")
        @Schema(description = "FCM 토큰", example = "eY5..._fw")
        String token
) {}
