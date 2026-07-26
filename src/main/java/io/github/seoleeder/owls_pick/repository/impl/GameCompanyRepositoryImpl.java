package io.github.seoleeder.owls_pick.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.entity.game.GameCompany;
import io.github.seoleeder.owls_pick.repository.custom.GameCompanyRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QCompany.company;
import static io.github.seoleeder.owls_pick.entity.game.QGameCompany.gameCompany;

@RequiredArgsConstructor
public class GameCompanyRepositoryImpl implements GameCompanyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 게임 ID에 해당하는 개발사/배급사 목록 조회
     * */
    @Override
    public List<GameCompany> findByGameIdWithCompany(Long gameId) {
        if (gameId == null) {
            return List.of();
        }

        return queryFactory
                .selectFrom(gameCompany)
                .join(gameCompany.company, company).fetchJoin()
                .where(gameCompany.game.id.eq(gameId))
                .fetch();
    }
}
