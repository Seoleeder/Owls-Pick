package io.github.seoleeder.owls_pick.global.util; // 혹은 component, mapper 등 적절한 패키지

import io.github.seoleeder.owls_pick.dto.response.GameResponse;
import io.github.seoleeder.owls_pick.dto.response.UpcomingGameResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto; // 명칭 맞춤
import io.github.seoleeder.owls_pick.service.GamePriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameResponseConverter {

    private final GamePriceService gamePriceService;
    private final IgdbImageUrlProvider imageUrlProvider;

    /**
     * Page<GameWithReviewStatDto> 를 Page<GameResponse> 로 일괄 변환 (최저가 조인 포함)
     */
    public Page<GameResponse> convertPage(Page<GameWithReviewStatDto> rawPage) {
        if (rawPage.isEmpty()) {
            return rawPage.map(result -> convertToDto(result, null));
        }

        // 현재 페이지에 있는 게임들의 ID만 추출
        List<Long> gameIds = rawPage.getContent().stream()
                .map(result -> result.game().getId())
                .toList();

        // 게임 ID와 현재 최저가 매핑
        Map<Long, StoreDetail> lowestPriceMap = gamePriceService.getLowestPriceMap(gameIds);

        // DTO로 변환하여 새로운 Page 객체 반환
        return rawPage.map(result -> convertToDto(result, lowestPriceMap.get(result.game().getId())));
    }

    /**
     * GameWithReviewStatDto + 스토어 현재 최저가 데이터 -> GameResponse
     */
    public GameResponse convertToDto(GameWithReviewStatDto result, StoreDetail bestPrice) {
        Game game = result.game();
        ReviewStat reviewStat = result.reviewStat();

        return GameResponse.builder()
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

    /**
     * Entity(Game) -> UpcomingGameResponseDto (출시 예정작 전용 Dto)
     */
    public UpcomingGameResponse convertToUpcomingDto(Game game) {
        return UpcomingGameResponse.builder()
                .gameId(game.getId())
                .title(game.getTitle())
                .coverUrl(imageUrlProvider.generateImageUrl(game.getCoverId()))
                .firstRelease(game.getFirstRelease())
                .hypes(game.getHypes())
                .platforms(game.getPlatform() != null ? game.getPlatform() : Collections.emptyList())
                .build();
    }
}