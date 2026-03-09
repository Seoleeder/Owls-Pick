package io.github.seoleeder.owls_pick.dto.request;

import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "게임 통합 검색 및 필터링 조건 DTO")
public record GameSearchConditionRequest(
        @Schema(description = "검색 키워드 (제목)", example = "Elden Ring")
        @NotBlank(message = "Search keyword must not be blank.")
        @Size(min = 2, message = "Keyword must be at least 2 characters long.")
        String keyword,

        @Schema(description = "선택된 장르 목록", example = "[\"INDIE\", \"ADVENTURE\"]")
        List<GenreType> genres,

        @Schema(description = "선택된 테마 목록", example = "[\"ACTION\", \"FANTASY\"]")
        List<ThemeType> themes,

        @Schema(description = "최소 가격", example = "0")
        Integer minPrice,

        @Schema(description = "최대 가격", example = "100000")
        Integer maxPrice,

        @Schema(description = "최소 플레이타임 (시간)", example = "10")
        Integer minPlaytime,

        @Schema(description = "최대 플레이타임 (시간)", example = "100")
        Integer maxPlaytime,

        @Schema(description = "할인 중인 게임만 보기 여부", example = "true")
        Boolean isDiscounting,

        @Schema(description = "정렬 기준 (인기순, 최신순, 오래된순 등)", example = "POPULAR")
        GameSortType sort
) {
}
