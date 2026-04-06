package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "스팀 리뷰 요약 요청 DTO")
public record ReviewSummaryRequest(
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
