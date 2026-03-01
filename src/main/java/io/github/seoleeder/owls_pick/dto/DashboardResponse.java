package io.github.seoleeder.owls_pick.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "대시보드 응답용 DTO", description = "스팀 대시보드 데이터 응답용 & 캐싱에 쓰이는 DTO")
public record DashboardResponse(

        @Schema(description = "큐레이션 타입", example = "WEEKLY_TOP_SELLER")
        String curationType,            // 게임 큐레이션 타입 (주간, 월간 인기 게임 등)

        @Schema(description = "데이터 수집 기준 시각", example = "2026-02-12T10:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime referenceAt,     // 현재 조회된 차트의 기준 시간

        @Schema(description = "이전 차트 수집 기준 시각")
        LocalDateTime previousDate,    // 이전 차트 기준 시간 (슬라이드 '<' 버튼용, 없으면 null)

        @Schema(description = "다음 차트 수집 기준 시각")
        LocalDateTime nextDate,        // 다음 차트 기준 시간 (슬라이드 '>' 버튼용, 없으면 null)


        List<DashboardGameDto> games

) {
    public record DashboardGameDto(
            @Schema(description = "게임 ID", example = "1031")
            Long gameId,

            @Schema(description = "큐레이션 내 순위", example = "1")
            int rank,                   // 큐레이션 내 순위

            @Schema(description = "게임 타이틀", example = "EA SPORTS FC 26")
            String title,               // 게임명

            @Schema(description = "게임 커버 이미지 Url", example = "ehdudl")
            String coverUrl,            // 게임 커버 URL

            @Schema(description = "정가 (원)", example = "66000")
            int originalPrice,          // 정가

            @Schema(description = "할인가 (원)", example = "33000")
            int discountPrice,          // 할인가 (할인 되지 않울 경우 정가와 동일)

            @Schema(description = "할인율 (%)", example = "50")
            int discountRate            // 할인율 (할인 되지 않을 경우 0)
    ){}
}
