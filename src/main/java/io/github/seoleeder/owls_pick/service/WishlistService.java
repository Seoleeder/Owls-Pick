package io.github.seoleeder.owls_pick.service;

import io.github.seoleeder.owls_pick.dto.response.WishlistToggleResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.user.User;
import io.github.seoleeder.owls_pick.entity.user.Wishlist;
import io.github.seoleeder.owls_pick.entity.user.WishlistId;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import io.github.seoleeder.owls_pick.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    /**
     * 위시리스트 토글 메서드
     * 사용자의 위시리스트를 확인하여 이미 등록된 상태면 해제하고, 아니면 추가
     * 처리 후 해당 게임이 위시리스트에 담긴 총 횟수를 반환
     */
    @Transactional
    public WishlistToggleResponse toggleWishlist(Long userId, Long gameId) {
        WishlistId wishlistId = new WishlistId(userId, gameId);

        return wishlistRepository.findById(wishlistId)
                .map(wishlist -> {
                    // 이미 담긴 상태 -> 위시리스트 해제
                    wishlistRepository.delete(wishlist);
                    wishlistRepository.flush(); // 카운트 조회를 위해 DB 즉시 동기화

                    long totalCount = wishlistRepository.countByGameId(gameId);
                    log.info("Removed from wishlist - userId: {}, gameId: {}, current total: {}", userId, gameId, totalCount);

                    return new WishlistToggleResponse(false, totalCount);
                })
                .orElseGet(() -> {
                    // 추가되지 않은 상태 -> 위시리스트 추가
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
                    Game game = gameRepository.findById(gameId)
                            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_GAME));

                    Wishlist newWishlist = Wishlist.builder()
                            .id(wishlistId)
                            .user(user)
                            .game(game)
                            .build();

                    wishlistRepository.save(newWishlist);
                    wishlistRepository.flush(); // 카운트 조회를 위해 DB 즉시 동기화

                    long totalCount = wishlistRepository.countByGameId(gameId);
                    log.info("Added to wishlist - userId: {}, gameId: {}, current total: {}", userId, gameId, totalCount);

                    return new WishlistToggleResponse(true, totalCount);
                });
    }
}
