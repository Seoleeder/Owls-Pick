package io.github.seoleeder.owls_pick.repository.support;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import io.github.seoleeder.owls_pick.dto.request.GameSearchConditionRequest;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;
import static io.github.seoleeder.owls_pick.entity.game.QPlaytime.playtime;
import static io.github.seoleeder.owls_pick.entity.game.QReviewStat.reviewStat;
import static io.github.seoleeder.owls_pick.entity.game.QStoreDetail.storeDetail;
import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;

/**
 * Game 도메인용 헬퍼 클래스 모음
 */

@Component
public class GameExpressions {

    // --- 공통 상태 필터 ---

    /**
     * 현재 시각 기준으로 출시된 게임들만 필터링
     *
     */
    public BooleanExpression isReleased() {

        // 오늘 기준으로 하루 전(-1일)까지만 '확실히 출시된 게임'으로 간주
        LocalDate safeReleaseMargin = LocalDate.now().minusDays(1);

        // game.firstRelease <= (오늘 - 1일)
        return game.firstRelease.isNotNull().and(game.firstRelease.loe(safeReleaseMargin));
    }

    // --- 텍스트 검색 필터 ---

    /**
     * 검색어 포함 여부 및 유사도 기반 검색 (오타 허용)
     */
    public BooleanExpression titleContains(String keyword) {
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

    // --- 태그 및 배열 연산 필터 (PostgreSQL 연산자 활용)---

    /**
     * 해당 게임의 genre 배열에 해당 장르가 포함되어 있는지 확인
     *
     */
    public BooleanExpression containsGenre(GenreType genre) {
        if (genre == null) return null;
        // {0} = tag.genres (DB 컬럼, text[])
        // {1} = genre.getEngName() (검색어, 단일 String)
        // CAST(ARRAY[{1}] AS text[]) -> 배열 대 배열 비교를 통한 타입 매칭 에러 방지
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.genres, genre.getEngName());
    }

    /**
     * 해당 게임의 theme 배열에 해당 테마가 포함되어 있는지 확인
     *
     */
    public BooleanExpression containsTheme(ThemeType theme) {
        if (theme == null) return null;
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.themes, theme.getEngName());
    }

    /**
     * 해당 태그 이름이 장르나 테아메 포함되어 있는지 확인
     *
     */
    public BooleanExpression containsTag(String tagName) {
        if (!StringUtils.hasText(tagName)) return null;
        return Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.genres, tagName)
                .or(Expressions.booleanTemplate("function('array_contains', {0}, {1}) = true", tag.themes, tagName));
    }

    /**
     * 선택된 장르 중 하나라도 포함되어 있는지 검사 (교집합)
     */
    public BooleanExpression genresOverlap(List<GenreType> genres) {
        if (genres == null || genres.isEmpty()) return null;
        String[] genreNames = genres.stream().map(Enum::name).toArray(String[]::new);

        // PostgreSQL array_overlap 커스텀 함수 호출
        return Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.genres, genreNames);
    }

    /**
     * 선택된 테마 중 하나라도 포함되어 있는지 검사 (교집합)
     */
    public BooleanExpression themesOverlap(List<ThemeType> themes) {
        if (themes == null || themes.isEmpty()) return null;
        String[] themeNames = themes.stream().map(Enum::name).toArray(String[]::new);

        // PostgreSQL array_overlap 커스텀 함수 호출
        return Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.themes, themeNames);
    }

    /**
     * 태그가 하나라도 겹치는지 확인 (Overlap)
     *
     */
    public BooleanExpression tagsOverlap(String[] tags) {
        return Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.genres, tags)
                .or(Expressions.booleanTemplate("function('array_overlap', {0}, {1}) = true", tag.themes, tags));
    }

    /**
     * 태그 교집합 점수 계산 (장르 교집합 개수 + 테마 교집합 개수)
     */
    public NumberExpression<Integer> calculateTagMatchScore(String[] tags) {

        // // 장르(genres) 교집합 개수 산출
        NumberExpression<Integer> genreMatchCount = Expressions.numberTemplate(Integer.class,
                "cast(function('array_intersect_count', {0}, {1}) as int)",
                tag.genres, tags);

        // 테마(themes) 교집합 개수 산출
        NumberExpression<Integer> themeMatchCount = Expressions.numberTemplate(Integer.class,
                "cast(function('array_intersect_count', {0}, {1}) as int)",
                tag.themes, tags);

        // 산출된 가중치 총합 반환
        return genreMatchCount.add(themeMatchCount);
    }

    // --- EXISTS 조건 서브쿼리 ---

    /**
     * StoreDetail(1:N) 조건 필터링 EXISTS 서브쿼리
     */
    public BooleanExpression storeConditionExists(GameSearchConditionRequest condition) {
        if (condition.minPrice() == null && condition.maxPrice() == null && !Boolean.TRUE.equals(condition.isDiscounting())) {
            return null;
        }

        // [수정 완료] 할인가(discountPrice)가 null인 정가 판매 상품을 위해 coalesce 연산 사용
        NumberExpression<Integer> effectivePrice = storeDetail.discountPrice.coalesce(storeDetail.originalPrice);

        BooleanExpression priceBetween = (condition.minPrice() != null || condition.maxPrice() != null)
                ? effectivePrice.between(
                condition.minPrice() != null ? condition.minPrice() : 0,
                condition.maxPrice() != null ? condition.maxPrice() : Integer.MAX_VALUE
        )
                : null;

        BooleanExpression isDiscounting = Boolean.TRUE.equals(condition.isDiscounting())
                ? storeDetail.discountRate.gt(0)
                : null;

        return JPAExpressions
                .selectOne()
                .from(storeDetail)
                .where(
                        storeDetail.game.id.eq(game.id),
                        priceBetween,
                        isDiscounting
                )
                .exists();
    }

    /**
     * Playtime(1:N) 조건 필터링 EXISTS 서브쿼리
     */
    public BooleanExpression playtimeExists(GameSearchConditionRequest condition) {
        if (condition.minPlaytime() == null && condition.maxPlaytime() == null) {
            return null;
        }

        BooleanExpression playtimeBetween = playtime.mainStory.between(
                condition.minPlaytime() != null ? condition.minPlaytime() : 0,
                condition.maxPlaytime() != null ? condition.maxPlaytime() : Integer.MAX_VALUE
        );

        return JPAExpressions
                .selectOne()
                .from(playtime)
                .where(
                        playtime.id.eq(game.id),
                        playtimeBetween
                )
                .exists();
    }

    /**
     * Tag(1:1) 조건 필터링 EXISTS 서브쿼리
     */
    public BooleanExpression tagExists(GameSearchConditionRequest condition) {
        if ((condition.genres() == null || condition.genres().isEmpty()) &&
                (condition.themes() == null || condition.themes().isEmpty())) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(tag)
                .where(
                        tag.id.eq(game.id),
                        genresOverlap(condition.genres()),
                        themesOverlap(condition.themes())
                )
                .exists();
    }

    // --- 동적 정렬 (Sorting) ---

    /**
     * 동적 정렬 메서드
     */
    public OrderSpecifier<?>[] getOrderSpecifiers(GameSortType sort, String keyword) {

        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        // 검색어가 존재하는 경우: 완전 일치 및 유사도 우선 정렬
        if (StringUtils.hasText(keyword)) {
            // 대소문자 구분 없이 제목이 완벽히 일치하면 최상단(1)으로, 아니면 그 뒤(2)로 배치
            specifiers.add(new CaseBuilder()
                    .when(game.title.equalsIgnoreCase(keyword)).then(1)
                    .otherwise(2).asc());

            // 완벽 일치가 아니라면, 유사도가 높은 순으로 정렬
            specifiers.add(Expressions.numberTemplate(Double.class,
                    "function('similarity', {0}, {1})",
                    game.title, keyword).desc());
        }

        // 유저 선택 정렬 옵션 적용 (기본값: POPULAR)
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
