package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "키워드 한글화 요청 DTO")
public record KeywordLocalizationRequest(

        @Schema(description = "비동기 콜백 매핑용 요청 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        String requestId,

        @Schema(description = "작업 완료 후 결과를 수신할 Webhook URL", example = "http://owls-pick-api:8080/api/internal/callback/genai/keywords")
        String callbackUrl,

        @Schema(description = "한글화할 영문 키워드 목록")
        @NotEmpty(message = "Keywords list cannot be empty.")
        List<String> keywords
) {}
