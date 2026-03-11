package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.GameCompany;
import io.github.seoleeder.owls_pick.entity.game.GameCompanyId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameCompanyRepository extends JpaRepository<GameCompany, GameCompanyId> {
    void deleteByGameIn(List<Game> games);

    // GameCompany을 조회하면서 연결된 Company 엔티티까지 조인해서 가져옴
    @EntityGraph(attributePaths = {"company"})
    List<GameCompany> findByGameId(Long gameId);
}
