package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "위시리스트 응답 DTO (마이페이지)")
public record WishlistResponse(
        @Schema(description = "찜한 시각", example = "2026-03-12T15:28:30")
        LocalDateTime wishedAt,

        @Schema(description = "게임 응답 데이터 (게임 기본 정보, 리뷰 스탯, 가격)")
        GameResponse gameResponse
){
}
