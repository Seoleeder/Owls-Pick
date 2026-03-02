package io.github.seoleeder.owls_pick.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "게임 상세 정보 응답 DTO")
public record GameResponseDto(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,

        @Schema(description = "게임 타이틀", example = "Stardew Valley")
        String title,

        @Schema(description = "커버 이미지 URL", example = "https://images.igdb.com/...")
        String coverUrl,

        @Schema(description = "최초 발매일", example = "2018-10-16")
        LocalDate firstRelease,

        @Schema(description = "스팀 전체 리뷰 수", example = "1031")
        int totalReview,

        @Schema(description = "스팀 리뷰 스코어", example = "98")
        int reviewScore,

        @Schema(description = "정가", example = "16000")
        int originalPrice,

        @Schema(description = "현재 최저가(할인가)", example = "8000")
        int discountPrice,

        @Schema(description = "할인율(%)", example = "50")
        int discountRate
) {
}
