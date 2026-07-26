package io.github.seoleeder.owls_pick.repository.custom;

import io.github.seoleeder.owls_pick.entity.game.GameCompany;

import java.util.List;

public interface GameCompanyRepositoryCustom {
    /**
     *  특정 게임 ID에 해당하는 개발사/배급사 목록 조회
     * */
    List<GameCompany> findByGameIdWithCompany(Long gameId);
}
