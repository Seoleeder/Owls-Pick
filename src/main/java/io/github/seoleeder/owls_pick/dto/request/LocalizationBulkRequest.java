package io.github.seoleeder.owls_pick.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "한글화 엔진 대량 요청 DTO")
public record LocalizationBulkRequest(

        @Schema(description = "비동기 콜백 식별용 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String requestId,

        @Schema(description = "완료 후 결과를 전송할 웹훅 URL", example = "http://owls-pick-api:8081/api/internal/webhook/localization")
        String callbackUrl,

        @Schema(description = "한글화 요청할 게임 목록")
        @NotEmpty(message = "The game list cannot be empty.")
        @Valid
        List<GameItem> games
) {
    @Schema(description = "한글화 요청 개별 게임 데이터")
    public record GameItem(

            @Schema(description = "게임 ID", example = "190264")
            @NotNull(message = "Game ID is required.")
            Long gameId,

            @Schema(description = "원본 설명 (영문)", example = "A grand epic set in the wild west.")
            String description,

            @Schema(description = "원본 스토리라인 (영문)", example = "Arthur Morgan and the Van der Linde gang...")
            String storyline
    ) {}
}
