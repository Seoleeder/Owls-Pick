package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.config.TestQueryDSLConfig;
import io.github.seoleeder.owls_pick.entity.game.Dashboard;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.support.AbstractContainerBaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
//자동 인메모리 DB 연결 방지 -> AbstractContainerBaseTest에서 설정한 PostgreSQL 컨테이너를 사용하도록 강제함
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestQueryDSLConfig.class)
public class DashboardRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private GameRepository gameRepository;

    @Test
    @DisplayName("특정 큐레이션, 특정 집계 시각에 대한 게임 랭크 조회")
    void findLatestTop100Test() {
        // Given
        // 1. 게임 데이터 생성
        Game eldenRing = gameRepository.save(Game.builder().title("Elden Ring").build());
        Game darkSouls = gameRepository.save(Game.builder().title("Dark Souls").build());
        Game zelda = gameRepository.save(Game.builder().title("Zelda").build());

        // 2. 시간 설정 (마이크로초 절삭으로 DB와 싱크 맞춤)
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        LocalDateTime yesterday = now.minusDays(1).truncatedTo(ChronoUnit.MICROS);;

        // 3. 데이터 시나리오 생성

        // [Case 1] 어제 1등 (조회되면 안 됨 - 날짜 필터링)
        dashboardRepository.save(Dashboard.builder()
                .game(eldenRing)
                .curationType(Dashboard.CurationType.WEEKLY_TOP_SELLER)
                .rank(1)
                .referenceAt(yesterday)
                .updatedAt(yesterday)
                .build());

        // [Case 2] 오늘 1등 (조회 대상)
        dashboardRepository.save(Dashboard.builder()
                .game(eldenRing)
                .curationType(Dashboard.CurationType.WEEKLY_TOP_SELLER)
                .rank(1)
                .referenceAt(now)
                .updatedAt(now)
                .build());

        // [Case 3] 오늘 2등 (조회 대상)
        dashboardRepository.save(Dashboard.builder()
                .game(darkSouls)
                .curationType(Dashboard.CurationType.WEEKLY_TOP_SELLER)
                .rank(2)
                .referenceAt(now)
                .updatedAt(now)
                .build());

        // [Case 4] 오늘자 데이터지만 다른 타입 (조회되면 안 됨 - 타입 필터링)
        dashboardRepository.save(Dashboard.builder()
                .game(zelda)
                .curationType(Dashboard.CurationType.MONTHLY_TOP) // 다른 타입
                .rank(1)
                .referenceAt(now)
                .updatedAt(now)
                .build());

        // When
        List<Dashboard> result = dashboardRepository.findLatestTop100(Dashboard.CurationType.WEEKLY_TOP_SELLER);

        // Then
        assertThat(result).hasSize(2); // 오늘자 WEEKLY 2개만

        // 랭킹 순서대로 정렬되었는지 확인 (1위 -> 2위)
        assertThat(result.get(0).getGame().getTitle()).isEqualTo("Elden Ring");
        assertThat(result.get(1).getGame().getTitle()).isEqualTo("Dark Souls");
    }
}
