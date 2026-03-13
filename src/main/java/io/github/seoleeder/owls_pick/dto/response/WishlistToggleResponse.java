package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Schema(description = "게임 찜 토글 응답 DTO")
public record WishlistToggleResponse (
    @Schema(description = "현재 유저의 찜 상태 (true: 찜 추가됨, false: 찜 해제됨)", example = "true")
    boolean isWished,

    @Schema(description = "해당 게임을 찜한 총 유저 수", example = "1204")
    long totalWishCount
){}
