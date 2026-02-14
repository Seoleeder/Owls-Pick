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

        // 트랜잭션 템플릿 내부 로직 실행 보장 (lenient: 미호출 시 Stubbing 에러 방지)
        lenient().doAnswer(inv -> {
            Consumer<TransactionStatus> callback = inv.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("[실시간] 동시 접속자 데이터 수집, 저장 및 캐시 갱신 (DB에 없는 게임은 제외)")
    void syncConcurrentPlayers_Success() {
        // Given
        // 1. API 응답 (1위: 100번 게임, 2위: 200번 게임)
        List<Rank> ranks = List.of(
                new Rank(1, 100L),
                new Rank(2, 200L)
        );
        SteamDashboardResponse response = new SteamDashboardResponse(LocalDateTime.now(), ranks);

        given(collector.collectConcurrentPlayersTopApp("KR")).willReturn(response);

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

    @Test
    @DisplayName("[정기/주간] 해당 기간의 데이터가 없으면 수집 후 저장")
    void syncScheduledWeekly_Success() {
        // Given
        // 1. 중복 체크: DB에 해당 기간 데이터가 '없다(false)'고 가정 -> 수집 로직 실행됨
        given(dashboardRepository.existsByCurationTypeAndReferenceAt(eq(Dashboard.CurationType.WEEKLY_TOP_SELLER), any()))
                .willReturn(false);

        // 2. API 응답 (1위: 100번 게임)
        List<Rank> ranks = List.of(new Rank(1, 100L));
        SteamDashboardResponse response = new SteamDashboardResponse(LocalDateTime.now(), ranks);

        // 날짜 계산 로직은 서비스 내부이므로 인자는 any()로 유연하게 처리
        given(collector.collectWeeklyTopSellers(any(), any(), any(), any()))
                .willReturn(response);

        // 4. DB 조회 (게임 정보 매핑용)
        Game game = Game.builder().id(1L).title("Elden Ring").build();
        StoreDetail detail = StoreDetail.builder().game(game).storeAppId("100").build();

        given(storeDetailRepository.findByStoreNameAndStoreAppIdIn(eq(StoreDetail.StoreName.STEAM), anyList()))
                .willReturn(List.of(detail));

        // When
        steamDashboardSyncService.syncScheduledWeekly();

        // Then
        // 1. API 호출 검증 (중복 데이터가 없었으므로 호출되어야 함)
        verify(collector).collectWeeklyTopSellers(any(), any(), any(), any());

        // 2. 저장 검증: 타입(Weekly)과 랭킹이 정확히 들어갔는지 확인
        verify(dashboardRepository).saveAll(argThat(list -> {
            List<Dashboard> dashboards = (List<Dashboard>) list;
            return dashboards.size() == 1
                    && dashboards.get(0).getCurationType() == Dashboard.CurationType.WEEKLY_TOP_SELLER
                    && dashboards.get(0).getRank() == 1
                    && dashboards.get(0).getGame().getTitle().equals("Elden Ring");
        }));

        // 3. 캐시 갱신 검증
        verify(dashboardCacheService).refreshCache(Dashboard.CurationType.WEEKLY_TOP_SELLER);
    }

    @Test
    @DisplayName("[정기/주간] 이미 해당 기간의 데이터가 존재하면 API 호출을 건너뛴다 (중복 방지)")
    void syncScheduledWeekly_SkipIfExists() {
        // Given
        // 1. 중복 체크: DB에 이미 데이터가 '있다(true)'고 가정 -> 얼리 리턴(Early Return) 발동
        given(dashboardRepository.existsByCurationTypeAndReferenceAt(eq(Dashboard.CurationType.WEEKLY_TOP_SELLER), any()))
                .willReturn(true);

        // When
        steamDashboardSyncService.syncScheduledWeekly();

        // Then
        // 1. 핵심 검증: API 수집기가 절대로 호출되지 않아야 함 (네트워크 비용 절감 & 중복 방지)
        verify(collector, never()).collectWeeklyTopSellers(any(), any(), any(), any());

        // 2. 저장 로직도 실행되지 않아야 함
        verify(dashboardRepository, never()).saveAll(any());
    }

}