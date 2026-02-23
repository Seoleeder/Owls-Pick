package io.github.seoleeder.owls_pick.repository.Custom;

import io.github.seoleeder.owls_pick.entity.game.Game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepositoryCustom {

    // 최종 수정 시간 조회
    Optional<LocalDateTime> findMaxIgdbUpdatedAt();

}
