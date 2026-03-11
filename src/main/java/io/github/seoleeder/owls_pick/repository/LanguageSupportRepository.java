package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.LanguageSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageSupportRepository extends JpaRepository<LanguageSupport, Long> {
    void deleteByGameIn(List<Game> games);

    // 특정 게임의 모든 언어 지원 정보 조회
    List<LanguageSupport> findByGameId(Long gameId);
}
