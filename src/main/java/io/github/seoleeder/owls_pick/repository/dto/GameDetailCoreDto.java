package io.github.seoleeder.owls_pick.repository.dto;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.Playtime;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.Tag;

/**
 * 게임 상세 페이지 로딩 시, 1:1 관계의 핵심(Core) 데이터들만 한 번에 담아오는 DTO
 */
public record GameDetailCoreDto(
        Game game,
        ReviewStat reviewStat,
        Playtime playtime,
        Tag tag
) {}
