package io.github.seoleeder.owls_pick.repository.Custom;

import io.github.seoleeder.owls_pick.repository.dto.ExploreGameResult;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepositoryCustom {

    // 최종 수정 시간 조회
    Optional<LocalDateTime> findMaxIgdbUpdatedAt();

    // 특정 장르의 게임 조회 (정렬 기준 선택)
    Page<ExploreGameResult> findGamesByGenre(GenreType genre, GameSortType sort, Pageable pageable);

    // 특정 테마의 게임 조회 (정렬 기준 선택)
    Page<ExploreGameResult> findGamesByTheme(ThemeType theme, GameSortType sort, Pageable pageable);

}
