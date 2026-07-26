package io.github.seoleeder.owls_pick.repository.custom;

public interface FcmTokenRepositoryCustom {

    // 특정 사용자의 모든 기기 토큰 삭제
    long deleteAllByUserId(Long userId);
}
