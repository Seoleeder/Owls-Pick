package io.github.seoleeder.owls_pick.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "마이 페이지 통합 정보 응답 DTO")
public record MyPageResponse(
        @Schema(description = "가입한 소셜 제공자", example = "GOOGLE")
        String provider,

        @Schema(description = "닉네임", example = "도찌")
        String nickname,

        @Schema(description = "이메일", example = "owlspick1031@gmail.com")
        String email,

        @Schema(description = "할인 알림 수신 동의 여부", example = "true")
        boolean isDiscountNotificationEnabled,

        @Schema(description = "선호 게임 태그 목록", example = "[Indie, Action, RPG]")
        List<String> preferredTags,

        @Schema(description = "선호 게임 스토어 목록", example = "[Steam, Epic Games Store]")
        List<String> preferredStores
) {
}
