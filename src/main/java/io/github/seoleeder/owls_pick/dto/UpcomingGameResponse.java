package io.github.seoleeder.owls_pick.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "출시 예정 기대작 전용 응답 DTO")
public record UpcomingGameResponse(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,

        @Schema(description = "게임 타이틀", example = "Grand Theft Auto VI")
        String title,

        @Schema(description = "커버 이미지 URL", example = "https://images.igdb.com/...")
        String coverUrl,

        @Schema(description = "출시 예정일", example = "2026-11-29")
        LocalDate firstRelease,

        @Schema(description = "글로벌 유저 기대도 (Hypes)", example = "12500")
        Integer hypes,

        @Schema(description = "출시 예정 플랫폼 목록", example = "[\"PC\", \"PS5\", \"Xbox Series X\"]")
        List<String> platforms
) {}
