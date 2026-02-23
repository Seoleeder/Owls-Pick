package io.github.seoleeder.owls_pick.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.seoleeder.owls_pick.entity.game.Dashboard.CurationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(name = "대시보드 응답용 DTO", description = "스팀 대시보드 데이터 응답용 & 캐싱에 쓰이는 DTO")
public record DashboardCacheDto(

        @Schema(description = "게임 ID", example = "1031")
        Long gameId,

        @Schema(description = "게임 타이틀", example = "EA SPORTS FC 26")
        String title,

        @Schema(description = "게임 커버 이미지 ID", example = "gnuoyd")
        String coverId,

        @Schema(description = "큐레이션 타입", example = "WEEKLY_TOP_SELLER")
        CurationType curationType,

        @Schema(description = "현재 순위", example = "1")
        int rank,

        @Schema(description = "정가 (원)", example = "66000")
        Integer originalPrice,

        @Schema(description = "할인가 (원)", example = "33000")
        Integer discountPrice,

        @Schema(description = "할인율 (%)", example = "50")
        Integer discountRate,

        @Schema(description = "데이터 수집 기준 시각", example = "2026-02-12T10:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime referenceAt

) {
}
