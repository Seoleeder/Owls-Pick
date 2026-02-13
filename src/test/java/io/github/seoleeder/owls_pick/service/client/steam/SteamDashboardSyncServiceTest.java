package io.github.seoleeder.owls_pick.service.client.steam;

import io.github.seoleeder.owls_pick.client.steamweb.SteamDataCollector;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamDashboardResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.SteamDashboardResponse.Rank;
import io.github.seoleeder.owls_pick.common.config.properties.SteamProperties;
import io.github.seoleeder.owls_pick.entity.game.Dashboard;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.repository.DashboardRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import io.github.seoleeder.owls_pick.service.DashboardCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamDashboardSyncServiceTest {

    @InjectMocks
    private SteamDashboardSyncService steamDashboardSyncService;

    @Mock private SteamDataCollector collector;
    @Mock private StoreDetailRepository storeDetailRepository;
    @Mock private DashboardRepository dashboardRepository;
    @Mock private DashboardCacheService dashboardCacheService;
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        SteamProperties props = new SteamProperties(
                null,
                null,
                null,
                null,
                new SteamProperties.Dashboard(LocalDate.of(2022, 1, 1), "KR", 100)
        );

        steamDashboardSyncService = new SteamDashboardSyncService(
                collector,
                storeDetailRepository,
                dashboardRepository,
                dashboardCacheService,
                transactionTemplate,
                props
        );
    }

    @Test
    @DisplayName("[실시간] 동시 접속자 데이터를 수집하고, DB에 없는 게임은 제외 후 저장 및 캐시 갱신")
    void syncConcurrentPlayers_Success() {
        // Given
        // 1. API 응답 (1위: 100번 게임, 2위: 200번 게임)
        List<Rank> ranks = List.of(
                new Rank(1, 100L),
                new Rank(2, 200L)
        );
        SteamDashboardResponse response = new SteamDashboardResponse(LocalDateTime.now(), ranks);

        given(collector.collectConcurrentPlayersTopApp("KR")).willReturn(response);

        // 2. TransactionTemplate 실행 강제
        doAnswer(inv -> {
            Consumer<TransactionStatus> callback = inv.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 3. DB 조회 시나리오
        // "100번" 게임은 DB에 존재함 (Game 객체 생성)
        Game game100 = Game.builder().id(1L).title("PUBG").build();
        StoreDetail detail100 = StoreDetail.builder().game(game100).storeAppId("100").build();

        // "200번" 게임은 DB에 없음 (리스트에 포함 X -> 저장 제외되어야 함)

        given(storeDetailRepository.findByStoreNameAndStoreAppIdIn(eq(StoreDetail.StoreName.STEAM), anyList()))
                .willReturn(List.of(detail100)); // 100번만 리턴

        // When
        steamDashboardSyncService.syncRealTimeData(); // 내부적으로 syncConcurrentPlayers 호출

        // Then
        // 1. Dashboard 저장 검증 (200번은 제외되고, 100번 1개만 저장되어야 함)
        verify(dashboardRepository).saveAll(argThat(list -> {
            List<Dashboard> dashboards = (List<Dashboard>) list;
            return dashboards.size() == 1
                    && dashboards.get(0).getGame().getTitle().equals("PUBG")
                    && dashboards.get(0).getRank() == 1;
        }));

        // 2. Redis 캐시 갱신 호출 검증
        verify(dashboardCacheService).refreshCache(Dashboard.CurationType.CONCURRENT_PLAYER);
    }
}