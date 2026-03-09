package io.github.seoleeder.owls_pick.dto.response;

import io.github.seoleeder.owls_pick.entity.user.User;

public record UserStatusResponse(
        Long userId,
        boolean isOnboarded, // 온보딩 완료 여부
        boolean isAdult      // 연 나이 기준 성인 여부 (2026년 기준 2007년생부터 true)
) {
    public static UserStatusResponse from(User user) {
        return new UserStatusResponse(
                user.getId(),
                user.isOnboarded(),
                user.isAdultUser() // 우리가 만든 연 나이 계산 로직
        );
    }
}
