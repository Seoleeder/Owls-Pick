package io.github.seoleeder.owls_pick.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Gemini 한글화 서버 대량 요청 DTO")
public record BulkLocalizationRequest(

        @Schema(description = "한글화 요청할 게임 목록")
        @NotEmpty(message = "The game list cannot be empty.")
        @Valid
        List<GameItem> games
) {
    @Schema(description = "한글화 요청 개별 게임 데이터")
    public record GameItem(

            @Schema(description = "게임 ID", example = "190264")
            @NotNull(message = "Game ID is required.")
            @JsonProperty("game_id")
            Long gameId,

            @Schema(description = "원본 설명 (영문)", example = "A grand epic set in the wild west.")
            String description,

            @Schema(description = "원본 스토리라인 (영문)", example = "Arthur Morgan and the Van der Linde gang...")
            String storyline
    ) {}
}
