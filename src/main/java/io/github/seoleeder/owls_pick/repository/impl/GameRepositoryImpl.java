package io.github.seoleeder.owls_pick.repository.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.dto.embedding.EmbeddingSourceDto;
import io.github.seoleeder.owls_pick.dto.request.GameSearchConditionRequest;
import io.github.seoleeder.owls_pick.dto.response.SearchFilterMetadataResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.status.EmbeddingStatus;
import io.github.seoleeder.owls_pick.entity.genai.QGenaiFailedTask;
import io.github.seoleeder.owls_pick.entity.genai.enums.GenaiPipelineType;
import io.github.seoleeder.owls_pick.global.util.RestPage;
import io.github.seoleeder.owls_pick.repository.custom.GameRepositoryCustom;
import io.github.seoleeder.owls_pick.repository.dto.GameDetailCoreDto;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.repository.support.EmbeddingExpressions;
import io.github.seoleeder.owls_pick.repository.support.GameExpressions;
import io.github.seoleeder.owls_pick.repository.support.LocalizationExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
import static io.github.seoleeder.owls_pick.entity.game.QStoreDetail.storeDetail;
import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;
import static io.github.seoleeder.owls_pick.entity.game.QVectorEmbedding.vectorEmbedding;

@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final GameExpressions gameExpressions;
    private final LocalizationExpressions localizationExpressions;
    private final EmbeddingExpressions embeddingExpressions;


    /* 공통 쿼리 메서드 */

    /**
     * 가장 최근에 업데이트 된 IGDB 갱신 시각 조회
     * 스케줄러 동기화 기준 시각으로 활용
     * */
    @Override
    public Optional<LocalDateTime> findMaxIgdbUpdatedAt() {

        LocalDateTime maxDateTime = queryFactory
                .select(game.igdbUpdatedAt.max())
                .from(game)
                .fetchOne();

        return Optional.ofNullable(maxDateTime);
    }

    /* 외부 API 게임 데이터 동기화 쿼리 메서드 */
    /**
     * ITAD ID가 유효하게 존재하고 "NONE"이 아닌 게임을 커서 기반으로 지정된 limit만큼 조회
     */
    @Override
    public List<Game> findValidGamesWithItadId(Long lastId, int limit) {
        return queryFactory
                .selectFrom(game)
                .where(
                        game.itadId.isNotNull(),
                        game.itadId.ne("NONE"),
                        lastId > 0 ? game.id.gt(lastId) : null
                )
                .orderBy(game.id.asc())
                .limit(limit)
                .fetch();
    }

    /**
     * 특정 장르 + 테마의 조합을 가진 게임 수 집계
     * */
    @Override
    public long countGamesByGenreAndTheme(GenreType genre, ThemeType theme) {
        Long count = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .where(
                        gameExpressions.containsGenre(genre),
                        gameExpressions.containsTheme(theme),
                        gameExpressions.isReleased()
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    /* 장르/테마 태그 기반 탐색 쿼리 메서드 */

    /**
     * 특정 장르에 해당하는 게임 목록 조회
     */
    @Override
    public Page<GameWithReviewStatDto> findGamesByGenre(GenreType genre, GameSortType sort, Pageable pageable) {
        return findGamesByTagCondition(gameExpressions.containsGenre(genre), sort, pageable);
    }

    /**
     * 특정 테마에 해당하는 게임 목록 조회
     */
    @Override
    public Page<GameWithReviewStatDto> findGamesByTheme(ThemeType theme, GameSortType sort, Pageable pageable) {
        return findGamesByTagCondition(gameExpressions.containsTheme(theme), sort, pageable);
    }

    /**
     * 태그(장르/테마) 기반 게임 목록 및 카운트 조회 공통 처리
     */
    private Page<GameWithReviewStatDto> findGamesByTagCondition(BooleanExpression tagCondition, GameSortType sort, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(
                        GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        tagCondition,
                        gameExpressions.isReleased()
                )
                .orderBy(gameExpressions.getOrderSpecifiers(sort, null))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .where(
                        tagCondition,
                        gameExpressions.isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /* 사용자 맞춤 Pick 섹션 쿼리 메서드 */


    /**
     * 출시 예정인 게임 중, Hype 조건을 충족하는 게임 조회
     * */
    public Page<Game> findUpcomingGames(LocalDate today, LocalDate maxDate, int minHypes, Pageable pageable) {

        List<Game> content = queryFactory
                .selectFrom(game)
                .where(
                        game.firstRelease.between(today, maxDate),  // 지정된 기간 내 출시 예정 게임
                        game.coverId.isNotNull(),                   // 커버 이미지 존재 여부 확인
                        game.hypes.goe(minHypes)                    // 기준치 이상의 기대도를 가진 레코드 필터링
                )
                .orderBy(
                        game.hypes.desc(),              // 기대도 높은 순
                        game.firstRelease.asc()         // 출시 예정일 가까운 순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 산출 쿼리 실행
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .where(
                        game.firstRelease.between(today, maxDate),
                        game.coverId.isNotNull(),
                        game.hypes.goe(minHypes)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /* 개인화 맞춤형 게임 추천 섹션 쿼리 */

    /**
     * 사용자의 선호 태그 정보를 반영한 맞춤형 최적 게임 리스트 조회
     * 매개변수로 전달된 정렬된 태그 조합을 캐시 키로 활용 (TTL 30분)
     */
    @Override
    @Cacheable(value = "main_pick:personal", key = "#userTags + ':' + #pageable.pageNumber")
    public Page<GameWithReviewStatDto> findPersonalizedGamesByPreferredTags(List<String> userTags, Pageable pageable) {
        // 유저 태그 리스트를 배열 검색 규격(String[])으로 변환
        String[] tags = userTags.toArray(new String[0]);

        // 데이터 조회 쿼리 실행
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.tagsOverlap(tags), // 태그 배열 교집합 검증
                        gameExpressions.isReleased() // 출시된 게임만 노출
                )
                .orderBy(
                        gameExpressions.calculateTagMatchScore(tags).desc(),    // 일치하는 태그 교집합 수가 높은 순으로 정렬
                        reviewStat.totalReview.desc().nullsLast()   // 일치율 동일 시 누적 리뷰 수 내림차순 정렬
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 산출 쿼리 실행
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .where(
                        gameExpressions.tagsOverlap(tags),
                        gameExpressions.isReleased()
                );

        Page<GameWithReviewStatDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        // Redis 직렬화/역직렬화 규격 호환성을 위해 RestPage로 래핑
        return new RestPage<>(page);
    }

    /**
     * 특정 장르와 특정 테마의 조합(AND)을 가진 게임 조회
     * */
    @Override
    public Page<GameWithReviewStatDto> findGamesByGenreAndThemeIntersection(GenreType genre, ThemeType theme, Pageable pageable) {

        // 데이터 조회 쿼리 실행
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.containsGenre(genre), // 장르 포함
                        gameExpressions.containsTheme(theme), // 테마 포함
                        gameExpressions.isReleased()
                )
                .orderBy(reviewStat.totalReview.desc().nullsLast()) // 리뷰 많은 순으로 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 산출 쿼리 실행
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .where(
                        gameExpressions.containsGenre(genre),
                        gameExpressions.containsTheme(theme),
                        gameExpressions.isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 스팀의 숨겨진 명작 게임 조회
     * (리뷰 스코어 + 리뷰 수 로 필터링, 리뷰 스코어가 높으나 전체 리뷰 수가 상대적으로 적은 게임. TTL 24시간)
     */
    @Override
    @Cacheable(value = "main_pick:hidden_masterpiece", key = "#pageable.pageNumber")
    public Page<GameWithReviewStatDto> findHiddenMasterpieces(int minScore, int minReviews, int maxReviews, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        reviewStat.reviewScore.goe(minScore),         // 리뷰 스코어 8(매우 긍정적) 이상
                        reviewStat.totalReview.between(minReviews, maxReviews), // 리뷰 수로 필터링
                        gameExpressions.isReleased()
                )
                .orderBy(
                        reviewStat.reviewScore.desc(),         // 9점(압도적 긍정) 우선
                        reviewStat.totalReview.desc()           // 스코어가 같으면 리뷰가 더 많은 순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 산출 쿼리 실행
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        reviewStat.reviewScore.goe(minScore),
                        reviewStat.totalReview.between(minReviews, maxReviews),
                        gameExpressions.isReleased()
                );

        // 페이징 처리된 조회 결과 원본 객체 생성
        Page<GameWithReviewStatDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        // Redis 역직렬화 호환성을 위해 RestPage 타입으로 래핑 후 반환
        return new RestPage<>(page);
    }

    /**
     * 특정 태그가 포함된 게임을 주간 리뷰 수가 많은 순으로 조회
     * 검색 조건인 tagName을 캐시 키로 지정하여 유저 간 연산 결과 공유 (TTL 12시간)
     *
     */
    @Override
    @Cacheable(value = "main_pick:trending", key = "#tagName + ':' + #pageable.pageNumber")
    public Page<GameWithReviewStatDto> findTrendingGamesByTag(String tagName, int minScore, Pageable pageable) {

        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.containsTag(tagName),   // 태그 포함 검증
                        reviewStat.weeklyReview.gt(0),      // 최근 7일간 리뷰가 달린 게임 필터링
                        reviewStat.reviewScore.goe(minScore),   // 평점 하한선 (예: 7~8점)
                        gameExpressions.isReleased()
                )
                .orderBy(
                        reviewStat.weeklyReview.desc(),                         // 주간 리뷰 수 많은 순
                        reviewStat.totalReview.desc().nullsLast()               // 누적 리뷰 수 많은 순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 건수 산출 쿼리 실행
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .join(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.containsTag(tagName),
                        reviewStat.weeklyReview.gt(0),
                        reviewStat.reviewScore.goe(minScore),
                        gameExpressions.isReleased()
                );

        Page<GameWithReviewStatDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return new RestPage<>(page);
    }

    /**
     * 특정 태그가 포함된 플레이타임이 짧은 게임 조회
     * 트렌딩 픽과 동일한 생명주기(TTL 12시간)를 가지며, 독립적인 캐시 공간(quick_play)을 할당하여 데이터 충돌 방지
     */
    @Override
    @Cacheable(value = "main_pick:quick_play", key = "#tagName + ':' + #pageable.pageNumber")
    public Page<GameWithReviewStatDto> findShortPlaytimeGamesByTag(String tagName, int maxPlaytime, int minScore, Pageable pageable) {
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class, game, reviewStat))
                .from(game)
                .join(tag).on(tag.id.eq(game.id))
                .join(playtime).on(playtime.id.eq(game.id))
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.containsTag(tagName),
                        playtime.mainStory.between(5, maxPlaytime), // 최소 5분부터 maxPlaytime 이내의 게임
                        reviewStat.reviewScore.goe(minScore),       // 리뷰 스코어가 일정 수치 이상인 게임 필터링
                        gameExpressions.isReleased()
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
                .join(tag).on(tag.id.eq(game.id))
                .join(playtime).on(playtime.id.eq(game.id))
                .join(reviewStat).on(reviewStat.game.id.eq(game.id))
                .where(
                        gameExpressions.containsTag(tagName),
                        playtime.mainStory.between(5, maxPlaytime),
                        reviewStat.reviewScore.goe(minScore),
                        gameExpressions.isReleased()
                );

        // 페이징 처리된 조회 결과 원본 객체 생성
        Page<GameWithReviewStatDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        // Redis 역직렬화 호환성을 위해 RestPage 타입으로 래핑 후 반환
        return new RestPage<>(page);
    }

    /* 게임 통합 검색 및 필터 메타데이터 쿼리 메서드 */

    /**
     * 통합 검색 및 다중 조건(검색어, 태그, 가격, 플탐, 할인 등) 필터링
     */
    @Override
    public Page<GameWithReviewStatDto> searchGames(GameSearchConditionRequest condition, Pageable pageable) {

        // Phase 1: 고유 Game PK 페이징 선조회
        List<Long> gameIds = queryFactory
                .select(game.id)
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(
                        gameExpressions.titleContains(condition.keyword()),
                        gameExpressions.tagExists(condition),
                        gameExpressions.storeConditionExists(condition),
                        gameExpressions.playtimeExists(condition),
                        gameExpressions.isReleased()
                )
                .orderBy(gameExpressions.getOrderSpecifiers(condition.sort(), condition.keyword()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 조회 결과가 없으면 빈 페이지 반환
        if (gameIds.isEmpty()) {
            return PageableExecutionUtils.getPage(List.of(), pageable, () -> 0L);
        }

        // Phase 2: 식별된 gameIds 대상 상세 DTO 단독 조회
        List<GameWithReviewStatDto> content = queryFactory
                .select(Projections.constructor(GameWithReviewStatDto.class,
                        game,
                        reviewStat
                ))
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .where(game.id.in(gameIds))
                .orderBy(gameExpressions.getOrderSpecifiers(condition.sort(), condition.keyword()))
                .fetch();

        // 고유 Game 기준 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(game.count())
                .from(game)
                .where(
                        gameExpressions.titleContains(condition.keyword()),
                        gameExpressions.tagExists(condition),
                        gameExpressions.storeConditionExists(condition),
                        gameExpressions.playtimeExists(condition),
                        gameExpressions.isReleased()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * DB 내 실판매가 최소/최대 범위 조회
     */
    @Override
    public SearchFilterMetadataResponse.PriceRange getPriceRange() {
        // 할인가가 없으면 정가를 사용하도록 실판매가 기준 정의
        NumberExpression<Integer> effectivePrice = storeDetail.discountPrice.coalesce(storeDetail.originalPrice);

        // 최소/최대 실판매가 집계 쿼리 실행
        Tuple result = queryFactory
                .select(effectivePrice.min(), effectivePrice.max())
                .from(storeDetail)
                .fetchOne();

        // 조회 결과가 null이면 0으로 처리하여 DTO 반환
        return new SearchFilterMetadataResponse.PriceRange(
                result != null && result.get(0, Integer.class) != null ? result.get(0, Integer.class) : 0,
                result != null && result.get(1, Integer.class) != null ? result.get(1, Integer.class) : 0
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

    /**
     * 특정 게임의 핵심 상세 정보 조회
     * (특정 게임의 데이터와 1대1로 연관된 데이터들을 묶어서 조회)
     * */
    @Override
    public Optional<GameDetailCoreDto> findGameDetailCoreById(Long gameId) {
        GameDetailCoreDto result = queryFactory
                .select(Projections.constructor(GameDetailCoreDto.class,
                        game,
                        reviewStat,
                        playtime,
                        tag
                ))
                .from(game)
                .leftJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .leftJoin(playtime).on(playtime.id.eq(game.id))
                .leftJoin(tag).on(tag.id.eq(game.id))
                .where(game.id.eq(gameId))
                .fetchOne();

        return Optional.ofNullable(result);
    }


    /**
     * 한글화된 설명은 null이면서, 원문(description/storyline)은 존재하는 데이터만
     * 필터링하여 limit만큼 조회
     */
    @Override
    public List<Game> findUnlocalizedGames(int limit) {
        QGenaiFailedTask failedTask = QGenaiFailedTask.genaiFailedTask;

        return queryFactory
                .selectFrom(game)
                .where(
                        localizationExpressions.needsDescriptionLocalization(game)
                                .or(localizationExpressions.needsStorylineLocalization(game)),

                        // 미조치 실패 작업 목록에 존재하지 않는 데이터 필터링
                        JPAExpressions
                                .selectOne()
                                .from(failedTask)
                                .where(
                                        failedTask.targetId.eq(game.id),
                                        failedTask.pipelineType.eq(GenaiPipelineType.GAME_LOCALIZATION),
                                        failedTask.isHandled.eq(false)
                                )
                                .notExists()
                )
                .orderBy(game.id.asc())
                .limit(limit)
                .fetch();
    }

    /**
     * 임베딩 데이터가 존재하지 않는 게임 원본 데이터 조회
     */
    @Override
    public List<EmbeddingSourceDto> findGamesForEmbedding(int dbFetchSize) {
        return queryFactory
                // 임베딩 데이터 프로젝션 (Fallback용 영문 필드 포함)
                .select(
                        game.id,
                        game.title,
                        game.description,
                        game.descriptionKo,
                        tag.genres,
                        tag.themes,
                        tag.keywords,
                        tag.keywordsKo,
                        playtime.mainStory,
                        reviewStat.reviewScoreDesc,
                        reviewStat.reviewSummary
                )
                .from(game)
                // 태그, 리뷰 스탯, 플레이타임 엔티티 조인
                .innerJoin(tag).on(tag.id.eq(game.id))
                .innerJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .leftJoin(playtime).on(playtime.id.eq(game.id))    // Nullable 필드
                .leftJoin(vectorEmbedding).on(vectorEmbedding.game.id.eq(game.id))
                .where(
                        embeddingExpressions.isValidForEmbedding(),      // 임베딩 필수 데이터 검증
                        vectorEmbedding.isNull()
                )
                .orderBy(game.id.asc())
                .limit(dbFetchSize)
                .fetch().stream()
                // DTO 매핑 및 Fallback 처리 (한글 데이터 우선)
                .map(tuple -> EmbeddingSourceDto.of(
                        tuple.get(game.id),
                        tuple.get(game.title),
                        tuple.get(game.description),
                        tuple.get(game.descriptionKo),
                        tuple.get(tag.genres),
                        tuple.get(tag.themes),
                        tuple.get(tag.keywords),
                        tuple.get(tag.keywordsKo),
                        tuple.get(playtime.mainStory),
                        tuple.get(reviewStat.reviewScoreDesc),
                        tuple.get(reviewStat.reviewSummary)
                ))
                .toList();
    }

    /**
     * 임베딩 상태가 FAILED인 게임 원본 데이터 조회
     */
    @Override
    public List<EmbeddingSourceDto> findFailedGamesForEmbedding(int limit) {
        return queryFactory
                .select(
                        game.id,
                        game.title,
                        game.description,
                        game.descriptionKo,
                        tag.genres,
                        tag.themes,
                        tag.keywords,
                        tag.keywordsKo,
                        playtime.mainStory,
                        reviewStat.reviewScoreDesc,
                        reviewStat.reviewSummary
                )
                .from(game)
                .innerJoin(tag).on(tag.id.eq(game.id))
                .innerJoin(reviewStat).on(reviewStat.id.eq(game.id))
                .leftJoin(playtime).on(playtime.id.eq(game.id))
                .leftJoin(vectorEmbedding).on(vectorEmbedding.game.id.eq(game.id))
                .where(
                        embeddingExpressions.isValidForEmbedding(),
                        // 임베딩 실패(FAILED) 이력이 있는 데이터만 한정 조회
                        vectorEmbedding.embeddingStatus.eq(EmbeddingStatus.FAILED)
                )
                .orderBy(game.id.asc())
                .limit(limit)
                .fetch().stream()
                .map(tuple -> EmbeddingSourceDto.of(
                        tuple.get(game.id),
                        tuple.get(game.title),
                        tuple.get(game.description),
                        tuple.get(game.descriptionKo),
                        tuple.get(tag.genres),
                        tuple.get(tag.themes),
                        tuple.get(tag.keywords),
                        tuple.get(tag.keywordsKo),
                        tuple.get(playtime.mainStory),
                        tuple.get(reviewStat.reviewScoreDesc),
                        tuple.get(reviewStat.reviewSummary)
                ))
                .toList();
    }

}
