package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.dto.TagDto;
import io.github.seoleeder.owls_pick.dto.GameResponseDto;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.global.util.IgdbImageUrlProvider;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.dto.ExploreGameResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExploreService {

    private final GameRepository gameRepository;
    private final GamePriceService gamePriceService;
    private final IgdbImageUrlProvider imageUrlProvider;

    // 메인 페이지 노출용 메서드

    /**
     * 인기 장르 태그 조회 (isPopular 기반)
     */
    public List<TagDto> getPopularGenres() {
        return GenreType.getPopular().stream()
                .map(TagDto::from)
                .toList();
    }

    /**
     * 전체 장르 태그 조회
     */
    public List<TagDto> getAllGenres() {
        return Arrays.stream(GenreType.values())
                .map(TagDto::from)
                .toList();
    }

    /**
     * 인기 테마 태그 조회 (isPopular 기반)
     */
    public List<TagDto> getPopularThemes() {
        return ThemeType.getPopular().stream()
                .map(TagDto::from)
                .toList();
    }

    /**
     * 전체 테마 태그 조회
     */
    public List<TagDto> getAllThemes() {
        return Arrays.stream(ThemeType.values())
                .map(TagDto::from)
                .toList();
    }


    // 게임 리스트 조회 및 DTO 변환

    /**
     * 특정 장르의 게임 조회 (Pagination, 정렬 조건 적용) -> 게임 응답 DTO 변환
     * */
    public Page<GameResponseDto> getGamesByGenre(GenreType genre, GameSortType sort, Pageable pageable) {
        Page<ExploreGameResult> genrePage = gameRepository.findGamesByGenre(genre, sort, pageable);
        return enrichWithPrice(genrePage);
    }

    /**
     * 특정 장르의 게임 조회 (Pagination, 정렬 조건 적용) -> 게임 응답 DTO 변환
     * */
    public Page<GameResponseDto> getGamesByTheme(ThemeType theme, GameSortType sort, Pageable pageable) {
        Page<ExploreGameResult> themePage = gameRepository.findGamesByTheme(theme, sort, pageable);
        return enrichWithPrice(themePage);
    }

    /**
     * 조회된 게임 리스트에 스토어 최저가 데이터를 추가하여 최종 응답 페이지로 변환
     */
    private Page<GameResponseDto> enrichWithPrice(Page<ExploreGameResult> tagPage) {
        if (tagPage.isEmpty()) {
            return tagPage.map(result -> convertToDto(result, null));
        }

        // 득정 태그를 가진 게임 리스트에서 게임 ID 추출
        List<Long> gameIds = tagPage.getContent().stream()
                .map(result -> result.game().getId())
                .toList();

        // 게임 ID와 현재 최저가 매핑
        Map<Long, StoreDetail> lowestPriceMap = gamePriceService.getLowestPriceMap(gameIds);

        //응답 DTO 변환
        return tagPage.map(result -> convertToDto(result, lowestPriceMap.get(result.game().getId())));
    }

    /**
     * DB에서 가져온 게임 데이터와 스토어 현재 최저가 데이터로 응답 DTO 변환
     */
    private GameResponseDto convertToDto(ExploreGameResult result, StoreDetail bestPrice) {
        Game game = result.game();
        ReviewStat reviewStat = result.reviewStat();

        return GameResponseDto.builder()
                .gameId(game.getId())
                .title(game.getTitle())
                .coverUrl(imageUrlProvider.generateImageUrl(game.getCoverId()))
                .firstRelease(game.getFirstRelease())
                // 리뷰가 수집되지 않은 경우(Null) 0으로 기본값 방어
                .totalReview(reviewStat != null ? reviewStat.getTotalReview() : 0)
                .reviewScore(reviewStat != null ? reviewStat.getReviewScore() : 0)
                // 가격 정보가 없는 경우 기본값 0으로 설정
                .originalPrice((bestPrice != null && bestPrice.getOriginalPrice() != null) ? bestPrice.getOriginalPrice() : 0)
                .discountPrice((bestPrice != null && bestPrice.getDiscountPrice() != null) ? bestPrice.getDiscountPrice() : 0)
                .discountRate((bestPrice != null && bestPrice.getDiscountRate() != null) ? bestPrice.getDiscountRate() : 0)
                .build();
    }

}