package io.github.seoleeder.owls_pick.repository.Custom.game;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface GameMainPickRepositoryCustom {

    // 출시 예정 기대작 게임 조회
    Page<Game> findUpcomingGames(LocalDate today, LocalDate maxDate, int minHypes, Pageable pageable);

    // 선호 태그 기반 사용자 맞춤형 최적의 게임 조회
    Page<GameWithReviewStatDto> findPersonalizedGamesByPreferredTags(List<String> userTags, Pageable pageable);

    // 특정 장르와 특정 테마의 조합을 가진 게임 조회
    Page<GameWithReviewStatDto> findGamesByGenreAndThemeIntersection(GenreType genre, ThemeType theme, Pageable pageable);

    // 리뷰 스코어는 높은데 리뷰 수가 적은 숨겨진 명작 게임 조회
    Page<GameWithReviewStatDto> findHiddenMasterpieces(int minScore, int minReviews, int maxReviews, Pageable pageable);

    // 최근에 리뷰가 많이 달린 특정 태그의 게임 조회
    Page<GameWithReviewStatDto> findTrendingGamesByTag(String tagName, int minScore, Pageable pageable);

    // 플레이 타입이 짧으면서 스코어가 높은 게임 조회
    Page<GameWithReviewStatDto> findShortPlaytimeGamesByTag(String tagName, int maxPlaytime, int minScore, Pageable pageable);

    // 특정 장르 + 테마 조합을 가진 게임 수 집계
    long countGamesByGenreAndTheme(GenreType genre, ThemeType theme);
}
