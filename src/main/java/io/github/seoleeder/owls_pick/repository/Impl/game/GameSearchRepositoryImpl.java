package io.github.seoleeder.owls_pick.repository.Impl.game;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.dto.request.GameSearchConditionRequest;
import io.github.seoleeder.owls_pick.dto.response.SearchFilterMetadataResponse;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.repository.Custom.game.GameSearchRepositoryCustom;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;
import static io.github.seoleeder.owls_pick.entity.game.QPlaytime.playtime;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;
import static io.github.seoleeder.owls_pick.entity.game.QStoreDetail.storeDetail;
import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;

@RequiredArgsConstructor
public class GameSearchRepositoryImpl implements GameSearchRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GameWithReviewStatDto> searchGames(GameSearchConditionRequest condition, Pageable pageable) {

        // 메인 데이터 조회 쿼리 (통합 검색 및 필터링)
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id)) // 태그(배열) 검색을 위한 조인
                .leftJoin(reviewStat).on(reviewStat.game.id.eq(game.id))
                .leftJoin(storeDetail).on(storeDetail.game.id.eq(game.id)) // 가격 필터링을 위한 조인
                .leftJoin(playtime).on(playtime.game.id.eq(game.id))
                .where(
                        titleContains(condition.keyword()), // 제목 유사도 검색
                        genresOverlap(condition.genres()), // 장르 교집합 검사
                        themesOverlap(condition.themes()), // 테마 교집합 검사
                        priceBetween(condition.minPrice(), condition.maxPrice()), // 가격 필터링
                        playtimeBetween(condition.minPlaytime(), condition.maxPlaytime()), // 플레이 타임 필터링
                        isDiscounting(condition.isDiscounting()),   // 할인 중인 게임 필터링
                        isReleased() // 출시 완료된 게임만 노출
                )
                .distinct() // 조인으로 인한 중복 데이터 제거
                .orderBy(getOrderSpecifiers(condition.sort(), condition.keyword()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리 (페이징 최적화를 위해 별도 실행)
        JPAQuery<Long> countQuery = queryFactory
                .select(game.countDistinct())
                .from(game)
                .join(tag).on(tag.game.id.eq(game.id))
                .leftJoin(storeDetail).on(storeDetail.game.id.eq(game.id))
                .where(
                        titleContains(condition.keyword()),
                        genresOverlap(condition.genres()),
                        themesOverlap(condition.themes()),
                        priceBetween(condition.minPrice(), condition.maxPrice()),
                        playtimeBetween(condition.minPlaytime(), condition.maxPlaytime()),
                        isDiscounting(condition.isDiscounting()),
                        isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 현재 DB 내 가격 범위 조회
     * */
    @Override
    public SearchFilterMetadataResponse.PriceRange getPriceRange() {
        Tuple result = queryFactory
                .select(storeDetail.discountPrice.min(), storeDetail.discountPrice.max())
                .from(storeDetail)
                .fetchOne();

        return new SearchFilterMetadataResponse.PriceRange(
                result != null ? result.get(0, Integer.class) : 0,
                result != null ? result.get(1, Integer.class) : 0
        );
    }

    /**
     * 현재 DB 내 플레이타임 전체 범위 조회
     * */
    @Override
    public SearchFilterMetadataResponse.PlaytimeRange getPlaytimeRange() {
        // Game 엔티티를 거치지 않고 Playtime 테이블에서 직접 집계
        Tuple result = queryFactory
                .select(playtime.mainStory.min(), playtime.mainStory.max())
                .from(playtime)
                .fetchOne();

        return new SearchFilterMetadataResponse.PlaytimeRange(
                result != null && result.get(0, Integer.class) != null ? result.get(0, Integer.class) : 0,
                result != null && result.get(1, Integer.class) != null ? result.get(1, Integer.class) : 0
        );
    }

    // --- 동적 쿼리용 헬퍼 메서드 ---

    /**
     * 검색어 포함 여부 및 유사도 기반 검색 (오타 허용)
     */
    private BooleanExpression titleContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        // 대소문자 구분 없이 포함되는지 검사
        BooleanExpression contains = game.title.containsIgnoreCase(keyword);

        // 제목이 검색어와 일정 수준 이상 유사한지 검사
        BooleanExpression similar = Expressions.booleanTemplate(
                "function('similarity', {0}, {1}) > 0.3",
                game.title,
                keyword
        );

        return contains.or(similar);
    }

    /**
     * 선택된 장르 중 하나라도 포함되어 있는지 검사 (교집합)
     */
    private BooleanExpression genresOverlap(List<GenreType> genres) {
        if (genres == null || genres.isEmpty()) return null;
        String[] genreNames = genres.stream().map(Enum::name).toArray(String[]::new);

        // PostgreSQL array_overlap 커스텀 함수 호출
        return Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.genres, genreNames);
    }

    /**
     * 선택된 테마 중 하나라도 포함되어 있는지 검사 (교집합)
     */
    private BooleanExpression themesOverlap(List<ThemeType> themes) {
        if (themes == null || themes.isEmpty()) return null;
        String[] themeNames = themes.stream().map(Enum::name).toArray(String[]::new);

        // PostgreSQL array_overlap 커스텀 함수 호출
        return Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.themes, themeNames);
    }

    /**
     * 실제 할인 가격 범위 필터링
     */
    private BooleanExpression priceBetween(Integer min, Integer max) {
        if (min == null && max == null) return null;
        return storeDetail.discountPrice.between(
                min != null ? min : 0,
                max != null ? max : Integer.MAX_VALUE
        );
    }

    /**
     * 메인 스토리 기준 플레이타임 범위 필터링
     */
    private BooleanExpression playtimeBetween(Integer min, Integer max) {
        if (min == null && max == null) return null;

        return playtime.mainStory.between(
                min != null ? min : 0,
                max != null ? max : Integer.MAX_VALUE
        );
    }

    /**
     * 할인 중인 상품만 필터링 (할인율 0 초과)
     */
    private BooleanExpression isDiscounting(Boolean isDiscounting) {
        return Boolean.TRUE.equals(isDiscounting) ? storeDetail.discountRate.gt(0) : null;
    }

    /**
     * 현재 시점 기준 출시가 완료된 게임 필터링
     */
    private BooleanExpression isReleased() {
        return game.firstRelease.isNotNull().and(game.firstRelease.loe(java.time.LocalDate.now()));
    }

    /**
     * 동적 정렬 메서드
     */

    private OrderSpecifier<?>[] getOrderSpecifiers(GameSortType sort, String keyword) {
        // 기본 정렬 리스트 준비
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        // 검색어가 있으면 검색어와 제목의 유사도가 높은 순으로 우선 배치
        if (StringUtils.hasText(keyword)) {
            // 대소문자 구분 없이 제목이 완벽히 일치하면 최상단(1)으로, 아니면 그 뒤(2)로 배치
            specifiers.add(new CaseBuilder()
                    .when(game.title.equalsIgnoreCase(keyword)).then(1)
                    .otherwise(2).asc());

            // 완벽 일치가 아니라면, 유사도가 높은 순서대로 정렬
            specifiers.add(Expressions.numberTemplate(Double.class,
                    "function('similarity', {0}, {1})",
                    game.title, keyword).desc());
        }

        // 유저가 선택한 정렬 옵션 적용 (기본값: 인기순)
        GameSortType finalSort = (sort == null) ? GameSortType.POPULAR : sort;

        switch (finalSort) {
            case NEWEST -> specifiers.add(game.firstRelease.desc().nullsLast());
            case OLDEST -> specifiers.add(game.firstRelease.asc().nullsLast());
            case TITLE_ASC -> specifiers.add(game.title.asc());
            case POPULAR -> {
                specifiers.add(reviewStat.totalReview.desc().nullsLast()); // 전체 리뷰 수
                specifiers.add(reviewStat.reviewScore.desc().nullsLast()); // 리뷰 평점
                specifiers.add(game.firstRelease.desc().nullsLast());      // 최신순
            }
        }

        // 동일 조건 시 ID 순
        specifiers.add(game.id.desc());

        return specifiers.toArray(new OrderSpecifier[0]);
    }
}
