package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "검색 필터 메타데이터 응답 (슬라이더 범위 및 태그 목록)")
public record SearchFilterMetadataResponse(
        @Schema(description = "전체 장르 목록")
        List<GenreInfo> genres,

        @Schema(description = "전체 테마 목록")
        List<ThemeInfo> themes,

        @Schema(description = "DB 내 가격 분포 범위")
        PriceRange priceRange,

        @Schema(description = "DB 내 플레이타임 분포 범위")
        PlaytimeRange playtimeRange
) {
    @Builder
    public record GenreInfo(
            @Schema(description = "장르 코드", example = "INDIE") String code,
            @Schema(description = "장르 한글명", example = "인디") String korName
    ) {}

    @Builder
    public record ThemeInfo(
            @Schema(description = "테마 코드", example = "FANTASY") String code,
            @Schema(description = "테마 한글명", example = "판타지") String korName
    ) {}

    @Builder
    public record PriceRange(
            @Schema(description = "전체 게임 중 최소 가격", example = "0") Integer min,
            @Schema(description = "전체 게임 중 최대 가격", example = "250000") Integer max
    ) {}

    @Builder
    public record PlaytimeRange(
            @Schema(description = "전체 게임 중 최소 플레이타임", example = "1") Integer min,
            @Schema(description = "전체 게임 중 최대 플레이타임", example = "500") Integer max
    ) {}
}
