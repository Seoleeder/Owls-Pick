package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.config.TestQueryDSLConfig;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.support.AbstractContainerBaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MICROS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest // JPA 관련 빈만 로드
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 교체 방지 (Testcontainers 사용)
@Import(TestQueryDSLConfig.class)
public class GameRepositoryTest extends AbstractContainerBaseTest {
    @Autowired
    private GameRepository gameRepository;

    @Test
    @DisplayName("가장 최근에 업데이트된 IGDB 갱신 시각을 정확히 가져오는지 검증")
    void findMaxIgdbUpdatedAtTest() {
        // Given
        // 나노초를 마이크로초로 자르기
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).truncatedTo(MICROS);
        LocalDateTime today = LocalDateTime.now().truncatedTo(MICROS);

        // 게임 1: 어제 업데이트됨
        gameRepository.save(Game.builder()
                .title("Old Game")
                .igdbUpdatedAt(yesterday)
                .build());

        // 게임 2: 오늘 업데이트됨 (이게 조회되어야 함)
        gameRepository.save(Game.builder()
                .title("New Game")
                .igdbUpdatedAt(today)
                .build());

        // 게임 3: 업데이트 기록 없음 (null, 무시되어야 함)
        gameRepository.save(Game.builder()
                .title("Null Game")
                .igdbUpdatedAt(null)
                .build());

        // When
        Optional<LocalDateTime> result = gameRepository.findMaxIgdbUpdatedAt();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(today);
    }

    @Test
    @DisplayName("데이터가 하나도 없을 때는 빈 Optional을 반환")
    void findMaxIgdbUpdatedAt_EmptyTest() {
        // When
        Optional<LocalDateTime> result = gameRepository.findMaxIgdbUpdatedAt();

        // Then
        assertThat(result).isEmpty();
    }
}
