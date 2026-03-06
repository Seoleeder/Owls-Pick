package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.repository.Custom.GameRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;
import static io.github.seoleeder.owls_pick.entity.game.QPlaytime.playtime;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;
import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;

@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepositoryCustom {

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
     * 사용자의 선호 태그 정보를 반영한 맞춤형 최적 게임 리스트 조회
     * */
    @Override
    public Page<GameWithReviewStatDto> findPersonalizedGamesByPreferredTags(List<String> userTags, Pageable pageable) {
        // 유저 태그 리스트를 PostgreSQL 배열 규격에 맞게 변환
        String[] tags = userTags.toArray(new String[0]);

        // 컨텐츠 조회 쿼리
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        // 선호 태그 중 하나라도 겹치는 게 있는 게임들
                        Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.genres, tags)
                                .or(Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.themes, tags)),
                        isReleased() // 출시된 게임만 노출
                )
                .orderBy(
                        // '장르 교집합 개수 + 테마 교집합 개수' 가 높은 순으로 정렬
                        Expressions.numberTemplate(Integer.class,
                                "cardinality(array(select unnest({0}) intersect select unnest({1}))) + " +
                                        "cardinality(array(select unnest({2}) intersect select unnest({1})))",
                                tag.genres, tags, tag.themes).desc(),
                        // 가중치가 같을 경우 인기(리뷰 수) 순으로 정렬
                        reviewStat.totalReview.desc().nullsLast()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .where(
                        Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.genres, tags)
                                .or(Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.themes, tags)),
                        isReleased()
                );

        // PageableExecutionUtils를 사용하여 Page 객체 생성
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 장르와 특정 테마의 조합(AND)을 가진 게임 조회
     * */
    @Override
    public Page<GameWithReviewStatDto> findGamesByGenreAndThemeIntersection(GenreType genre, ThemeType theme, Pageable pageable) {

        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(genre), // 장르 포함
                        containsTheme(theme), // 테마 포함
                        isReleased()
                )
                .orderBy(reviewStat.totalReview.desc().nullsLast()) // 리뷰 많은 순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .where(
                        containsGenre(genre),
                        containsTheme(theme),
                        isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 스팀의 숨겨진 명작 게임 조회
     * (리뷰 스코어 + 리뷰 수 로 필터링, 리뷰 스코어가 높은데 리뷰 수가 상대적으로 적은 게임들)
     * */
    @Override
    public Page<GameWithReviewStatDto> findHiddenMasterpieces(int minScore, int minReviews, int maxReviews, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        reviewStat.reviewScore.goe(minScore),         // 리뷰 스코어 8(매우 긍정적) 이상
                        reviewStat.totalReview.between(minReviews, maxReviews), // 리뷰 수로 필터링
                        isReleased()
                )
                .orderBy(
                        reviewStat.reviewScore.desc(),         // 9점(압도적 긍정) 우선
                        reviewStat.totalReview.desc()           // 스코어가 같으면 리뷰가 더 많은 순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        reviewStat.reviewScore.goe(minScore),
                        reviewStat.totalReview.between(minReviews, maxReviews),
                        isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 최근 리뷰 수가 많이 증가한 특정 태그의 게임 조회
     * */
    @Override
    public Page<GameWithReviewStatDto> findTrendingGamesByTag(String tagName, int minScore, Pageable pageable) {

        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(tagName).or(containsTheme(tagName)),      // 해당 태그 포함 여부
                        reviewStat.weeklyReview.gt(0),                     // 최근 7일간 리뷰가 달린 게임 필터링
                        reviewStat.reviewScore.goe(minScore),                   // 평점 하한선 (예: 7~8점)
                        isReleased()
                )
                .orderBy(
                        reviewStat.weeklyReview.desc(),                         // 주간 리뷰 수가 제일 많은 순
                        reviewStat.totalReview.desc().nullsLast()               // 같다면, 전체 리뷰가 많은 순으로
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(tagName).or(containsTheme(tagName)),
                        reviewStat.weeklyReview.gt(0),
                        reviewStat.reviewScore.goe(minScore),
                        isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 태그의 플레이타임이 짧은 게임 조회
     * */
    @Override
    public Page<GameWithReviewStatDto> findShortPlaytimeGamesByTag(String tagName, int maxPlaytime, int minScore, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .join(playtime).on(playtime.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(tagName).or(containsTheme(tagName)),
                        playtime.mainStory.between(5, maxPlaytime), // 최소 5분부터 maxPlaytime 이내의 게임
                        reviewStat.reviewScore.goe(minScore),       // 리뷰 스코어가 일정 수치 이상인 게임 필터링
                        isReleased()
                )
                .orderBy(
                        playtime.mainStory.asc(),                   // 플레이 타임이 짧은 순
                        reviewStat.reviewScore.desc(),              // 플탐이 같다면 리뷰 스코어 높은 순
                        reviewStat.totalReview.desc().nullsLast()   // 스코어도 같다면 전체 리뷰 순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .join(playtime).on(playtime.game.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        containsGenre(tagName).or(containsTheme(tagName)),
                        playtime.mainStory.between(5, maxPlaytime),
                        reviewStat.reviewScore.goe(minScore),
                        isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 장르 + 테마의 조합을 가진 게임 수 집계
     * */
    @Override
    public long countGamesByGenreAndTheme(GenreType genre, ThemeType theme) {
        Long count = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .where(
                        containsGenre(genre),
                        containsTheme(theme),
                        isReleased()
                )
                .fetchOne();

        return count == null ? 0L : count;
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

    //String 값도 받기 위해 Overload
    private BooleanExpression containsGenre(String genreName) {
        if (genreName == null) return null;
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.genres, genreName);
    }

    private BooleanExpression containsTheme(String themeName) {
        if (themeName == null) return null;
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.themes, themeName);
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
