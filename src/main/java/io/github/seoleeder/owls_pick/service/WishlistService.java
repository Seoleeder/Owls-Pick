package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.dto.response.GameResponse;
import io.github.seoleeder.owls_pick.dto.response.WishlistResponse;
import io.github.seoleeder.owls_pick.dto.response.WishlistToggleResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.user.User;
import io.github.seoleeder.owls_pick.entity.user.Wishlist;
import io.github.seoleeder.owls_pick.entity.user.WishlistId;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.global.util.GameResponseConverter;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import io.github.seoleeder.owls_pick.repository.WishlistRepository;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import io.github.seoleeder.owls_pick.repository.dto.WishlistQueryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GamePriceService gamePriceService;
    private final GameResponseConverter gameResponseConverter;

    // ==========================================
    // 1. 공통 & 게임 상세 페이지용 기능
    // ==========================================

    /**
     * 위시리스트 토글 메서드
     * 이미 등록된 경우 삭제하고, 미등록 상태인 경우 신규 추가 후 총 위시리스트 수 반환
     */
    @Transactional
    public WishlistToggleResponse toggleWishlist(Long userId, Long gameId) {

        // 유저 ID와 게임 ID 기반 식별자 복합키 생성
        WishlistId wishlistId = new WishlistId(userId, gameId);

        return wishlistRepository.findById(wishlistId)
                .map(wishlist -> {
                    // 위시리스트 삭제 처리
                    wishlistRepository.delete(wishlist);

                    // 카운트 조회를 위해 DB 즉시 동기화
                    wishlistRepository.flush();

                    // 해당 게임의 전체 위시리스트 수 집계
                    long totalCount = wishlistRepository.countByGameId(gameId);
                    log.info("[Wishlist] Removed from wishlist - userId: {}, gameId: {}, current total: {}", userId, gameId, totalCount);

                    return WishlistToggleResponse.builder()
                            .isWished(false)
                            .totalWishCount(totalCount).build();
                })
                .orElseGet(() -> {
                    // 인증된 유저는 DB 조회 없이 프록시 객체 참조로 대체
                    User user = userRepository.getReferenceById(userId);
                    Game game = gameRepository.findById(gameId)
                            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_GAME));

                    // 신규 위시리스트 엔티티 생성
                    Wishlist newWishlist = Wishlist.builder()
                            .id(wishlistId)
                            .user(user)
                            .game(game)
                            .build();

                    wishlistRepository.save(newWishlist);

                    // 카운트 조회를 위해 DB 즉시 동기화
                    wishlistRepository.flush();

                    // 해당 게임의 전체 위시리스트 수 집계
                    long totalCount = wishlistRepository.countByGameId(gameId);
                    log.info("[Wishlist] Added to wishlist - userId: {}, gameId: {}, current total: {}", userId, gameId, totalCount);

                    return WishlistToggleResponse.builder()
                            .isWished(true)
                            .totalWishCount(totalCount)
                            .build();
                });
    }


    // ==========================================
    // 2. 마이 페이지 전용 기능
    // ==========================================

    /**
     * 사용자의 위시리스트 목록 페이징 조회
     * GameResponse(게임 기본 정보, 리뷰 통계, 최저가)에 해당 게임을 찜한 시각을 결합하여 반환
     */
    @Transactional(readOnly = true)
    public Page<WishlistResponse> getMyWishlist(Long userId, Pageable pageable) {

        // 유저의 위시리스트 기본 데이터 페이징 조회
        Page<WishlistQueryDto> wishlistPage = wishlistRepository.findWishlistPageByUserId(userId, pageable);

        // 찜 목록이 비어있는 경우 즉시 빈 페이지 반환
        if (wishlistPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 현재 페이징 결과 내 게임 ID 목록 추출
        List<Long> gameIds = wishlistPage.getContent().stream()
                .map(dto -> dto.game().getId())
                .toList();

        // 각 게임별 현재 최저가 데이터 매핑
        Map<Long, StoreDetail> lowestPriceMap = gamePriceService.getLowestPriceMap(gameIds);

        // 응답 DTO 변환 및 결과 조합
        return wishlistPage.map(dto -> {
            // 게임별 최저가 데이터 매핑
            StoreDetail bestOffer = lowestPriceMap.get(dto.game().getId());

            // DTO 변환용 중간 전달 객체 생성
            GameWithReviewStatDto tempDto = new GameWithReviewStatDto(dto.game(), dto.reviewStat());

            // 최저가 포함 게임 응답 DTO 생성
            GameResponse gameResponse = gameResponseConverter.convertToDto(tempDto, bestOffer);

            return WishlistResponse.builder()
                    .wishedAt(dto.wishedAt())
                    .gameResponse(gameResponse)
                    .build();
        });
    }

    /**
     * 위시리스트 해제 (찜 목록에서 삭제)
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long gameId) {
        // 식별자 복합키 생성 및 단건 삭제 실행
        WishlistId wishlistId = new WishlistId(userId, gameId);

        wishlistRepository.deleteById(wishlistId);

        log.info("[Wishlist] Explicitly removed from wishlist in My Page - userId: {}, gameId: {}", userId, gameId);
    }

}
