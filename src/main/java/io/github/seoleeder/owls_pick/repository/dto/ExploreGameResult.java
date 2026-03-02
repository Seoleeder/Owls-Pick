package io.github.seoleeder.owls_pick.repository.dto;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;

/**
 * ExploreService로 데이터를 전달하기 위한 DTO
 */
public record ExploreGameResult(
        Game game,
        ReviewStat reviewStat
) {}
