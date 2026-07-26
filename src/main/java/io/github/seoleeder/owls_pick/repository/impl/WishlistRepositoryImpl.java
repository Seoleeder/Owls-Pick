package io.github.seoleeder.owls_pick.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.repository.custom.WishlistRepositoryCustom;
import io.github.seoleeder.owls_pick.repository.dto.WishlistQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;
import static io.github.seoleeder.owls_pick.entity.user.QUser.user;
import static io.github.seoleeder.owls_pick.entity.user.QWishlist.wishlist;

@RequiredArgsConstructor
public class WishlistRepositoryImpl implements WishlistRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 유저 식별자 기준 위시리스트 및 연관 게임, 리뷰 통계 페이징 조회
     */
    @Override
    public Page<WishlistQueryDto> findWishlistPageByUserId(Long userId, Pageable pageable) {
        // 위시리스트 데이터 목록 조회 쿼리
        List<WishlistQueryDto> content = queryFactory
                .select(Projections.constructor(WishlistQueryDto.class,
                        wishlist.createdAt, // 찜한 시각
                        game,               // 게임 엔티티
                        reviewStat))        // 리뷰 통계 엔티티
                .from(wishlist)
                .join(wishlist.game, game)  // 위시리스트와 게임 join
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(wishlist.user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(wishlist.createdAt.desc()) // 최신순 정렬
                .fetch();

        // 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(wishlist.count())
                .from(wishlist)
                .where(wishlist.user.id.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 게임을 위시리스트에 등록하고 할인 알림에 동의한 유저 ID 목록 조회
     */
    @Override
    public List<Long> findTargetUserIdsForDiscountPush(Long gameId) {
        return queryFactory
                .select(wishlist.id.userId)
                .from(wishlist)
                .join(wishlist.user, user)
                .where(
                        wishlist.id.gameId.eq(gameId),
                        user.isDiscountNotificationEnabled.isTrue()
                )
                .fetch();
    }
}

