package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "스팀 리뷰 요약 요청 DTO")
public record ReviewSummaryRequest(

        @Schema(description = "비동기 콜백 매핑용 요청 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        String requestId,

        @Schema(description = "작업 완료 후 결과를 수신할 웹훅 URL", example = "http://owls-pick-api:8081/api/internal/callback/genai/reviews")
        String callbackUrl,

        @Schema(description = "게임 ID", example = "152")
        @NotNull(message = "Game ID cannot be null")
        Long gameId,

        @Schema(description = "스팀 리뷰 스코어", example = "9")
        @NotNull(message = "Review Score cannot be null")
        int reviewScore,

        @Schema(description = "요약이 필요한 스팀 리뷰 샘플링 리스트")
        @NotEmpty(message = "Review texts cannot be empty")
        List<String> reviewTexts

) {
}
