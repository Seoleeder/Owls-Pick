package io.github.seoleeder.owls_pick.repository.Impl.game;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.repository.Custom.game.GameCommonRepositoryCustom;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;
import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;

@RequiredArgsConstructor
public class GameCommonRepositoryImpl implements GameCommonRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<LocalDateTime> findMaxIgdbUpdatedAt() {

        LocalDateTime maxDateTime = queryFactory
                .select(game.igdbUpdatedAt.max())
                .from(game)
                .fetchOne();

        return Optional.ofNullable(maxDateTime);
    }

    /**
     * 특정 장르에 해당하는 게임 목록 조회 (페이징 및 다중 정렬)
     */
    @Override
    public Page<GameWithReviewStatDto> findGamesByGenre(GenreType genre, GameSortType sort, Pageable pageable) {
        // 데이터를 가져오는 Main Query
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(
                        GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                // 장르 검색을 위해 Tag 테이블과 Inner Join
                .join(tag).on(tag.game.id.eq(game.id))
                // 리뷰 데이터가 없는 게임도 조회되어야 하므로 반드시 Left Join 사용
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(genre),
                        isReleased()
                )
                .orderBy(getOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 데이터 개수를 세는 Count Query
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .where(containsGenre(genre),
                        isReleased()
                );

        //PageableExecutionUtils를 사용하여 필요할 때만 count 쿼리 실행
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 테마에 해당하는 게임 목록 조회 (페이징 및 다중 정렬)
     */
    @Override
    public Page<GameWithReviewStatDto> findGamesByTheme(ThemeType theme, GameSortType sort, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(
                        GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                // 리뷰가 없는 게임 누락 방지를 위한 Left Join
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsTheme(theme),
                        isReleased()
                )
                .orderBy(getOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .where(
                        containsTheme(theme),
                        isReleased()
                );

        // PageableExecutionUtils를 사용하여 Page 객체 생성
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 헬퍼 메서드
     * */

    // 해당 게임의 genre 배열에 해당 장르가 포함되어 있는지 확인
    private BooleanExpression containsGenre(GenreType genre) {
        if (genre == null) return null;
        // {0} = tag.genres (DB 컬럼, text[])
        // {1} = genre.getEngName() (검색어, 단일 String)
        // CAST(ARRAY[{1}] AS text[]) -> 배열 대 배열 비교를 통한 타입 매칭 에러 방지
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.genres, genre.getEngName());
    }

    // 해당 게임의 theme 배열에 해당 테마가 포함되어 있는지 확인
    private BooleanExpression containsTheme(ThemeType theme) {
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.themes, theme.getEngName());
    }

    // 현재 시각 기준으로 출시된 게임들만 필터링
    private BooleanExpression isReleased() {

        // 오늘 기준으로 하루 전(-1일)까지만 '확실히 출시된 게임'으로 간주
        LocalDate safeReleaseMargin = LocalDate.now().minusDays(1);

        // game.firstRelease <= (오늘 - 1일)
        return game.firstRelease.isNotNull().and(game.firstRelease.loe(safeReleaseMargin));
    }

    // 동적 정렬 메서드
    private OrderSpecifier<?>[] getOrderSpecifiers(GameSortType sort) {
        if (sort == null) {
            sort = GameSortType.POPULAR; // 디폴트는 인기순
        }

        return switch (sort) {
            case NEWEST -> new OrderSpecifier[]{
                    game.firstRelease.desc().nullsLast()
            };
            case OLDEST -> new OrderSpecifier[]{
                    game.firstRelease.asc().nullsLast()
            };
            case TITLE_ASC -> new OrderSpecifier[]{
                    game.title.asc()
            };
            case POPULAR -> new OrderSpecifier[]{
                    reviewStat.totalReview.desc().nullsLast(), // 전체 리뷰 수
                    reviewStat.reviewScore.desc().nullsLast(), // 리뷰 평점
                    game.firstRelease.desc().nullsLast()       // 리뷰가 없다면 최신순
            };
        };
    }
}
