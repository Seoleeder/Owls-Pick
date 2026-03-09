package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.repository.Custom.ReviewStatRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import static io.github.seoleeder.owls_pick.entity.game.QReview.review;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;

@RequiredArgsConstructor
public class ReviewStatRepositoryImpl implements ReviewStatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 게임의 ID를 받아 주간 리뷰 수 업데이트
     * */
    @Override
    public void updateWeeklyReviewCount(Long gameId, LocalDateTime startTime) {

        // 해당 게임의 최근 N일간 작성된 리뷰 개수 집계
        Long count = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        review.game.id.eq(gameId),
                        review.writtenAt.goe(startTime) // startTime 이후 작성된 리뷰
                )
                .fetchOne();

        int weeklyCount = count != null ? count.intValue() : 0;

        // ReviewStat의 weeklyReview 업데이트
        queryFactory.update(reviewStat)
                .set(reviewStat.weeklyReview, weeklyCount)
                .where(reviewStat.game.id.eq(gameId))
                .execute();
    }

}
