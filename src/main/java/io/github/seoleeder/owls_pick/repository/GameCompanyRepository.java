package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.GameCompany;
import io.github.seoleeder.owls_pick.entity.game.GameCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameCompanyRepository extends JpaRepository<GameCompany, GameCompanyId> {
    void deleteByGameIn(List<Game> games);
}
