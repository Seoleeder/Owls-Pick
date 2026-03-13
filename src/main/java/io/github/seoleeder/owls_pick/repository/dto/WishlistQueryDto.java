package io.github.seoleeder.owls_pick.repository.dto;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 찜한 시각 + 게임 + 리뷰 스탯 데이터를 묶은 내부용 DTO
 */
@Builder
public record WishlistQueryDto(
        LocalDateTime wishedAt,
        Game game,
        ReviewStat reviewStat
) {
}
