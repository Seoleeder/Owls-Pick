package io.github.seoleeder.owls_pick.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.repository.custom.FcmTokenRepositoryCustom;
import lombok.RequiredArgsConstructor;

import static io.github.seoleeder.owls_pick.entity.notification.QFcmToken.fcmToken;

@RequiredArgsConstructor
public class FcmTokenRepositoryImpl implements FcmTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자의 모든 FCM 토큰 일괄 삭제
     */
    @Override
    public long deleteAllByUserId(Long userId) {
        return queryFactory
                .delete(fcmToken)
                .where(fcmToken.user.id.eq(userId))
                .execute();
    }
}
