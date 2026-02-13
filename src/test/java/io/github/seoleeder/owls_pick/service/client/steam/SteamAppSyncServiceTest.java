package io.github.seoleeder.owls_pick.service.client.steam;

import io.github.seoleeder.owls_pick.client.steamweb.SteamDataCollector;
import io.github.seoleeder.owls_pick.client.steamweb.dto.SteamAppListResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.SteamAppListResponse.Response;
import io.github.seoleeder.owls_pick.client.steamweb.dto.SteamAppListResponse.Response.App;
import io.github.seoleeder.owls_pick.client.steamweb.util.SteamGameUrlBuilder;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamAppSyncServiceTest {

    @InjectMocks
    private SteamAppSyncService steamAppSyncService;

    @Mock private SteamDataCollector collector;
    @Mock private GameRepository gameRepository;
    @Mock private StoreDetailRepository storeDetailRepository;
    @Mock private SteamGameUrlBuilder urlBuilder;
    @Mock private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("[동기화 성공] DB에 없는 새로운 게임만 필터링하여 저장해야 한다")
    void syncAppListSuccessSaveNewGames() {

        // Given
        // 실제 DB에 접근하지 말고 임의 값 반환
        // Steam Id: "100" 이미 존재함
        given(storeDetailRepository.findAllAppIdsByStore(StoreDetail.StoreName.STEAM))
                .willReturn(Set.of("100"));

        // API 응답 시나리오
        // 앱 100: 이미 있음
        // 앱 200: 새로움 -> 저장
        // 앱 300: 이름 없음
        List<App> apps = List.of(
                new App(100L, "Existing Game"),
                new App(200L, "New Game"),
                new App(300L, "")
        );


        // 테스트용 API 응답 데이터 (haveMoreResults=false)
        Response responseBody = new Response(apps, false, 200L);
        SteamAppListResponse responseWrapper = new SteamAppListResponse(responseBody);

        //서비스가 스팀 API를 호출하는 부분 모킹
        given(collector.collectAppList(null)).willReturn(responseWrapper);

        // TransactionTemplate 모킹
        // executeWithoutResult가 호출되면, 내부의 람다(Consumer)를 강제로 실행시킴
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null); // 람다 실행 -> saveNewApps 호출됨
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // saveAll 모킹 (Game 저장 후 StoreDetail 저장 흐름을 위해 반환값 설정)
        Game savedGame = Game.builder().title("New Game").build();
        given(gameRepository.saveAll(anyList())).willReturn(List.of(savedGame));
        given(urlBuilder.buildUrl("200")).willReturn("https://store.steampowered.com/app/200");

        // When
        steamAppSyncService.syncAppList();

        // Then
        // Game은 1개만 저장되어야 함 ("New Game" - ID 200)
        verify(gameRepository, times(1)).saveAll(argThat(list -> {
            List<Game> games = (List<Game>) list;
            return games.size() == 1 && games.get(0).getTitle().equals("New Game");
        }));

        // StoreDetail도 1개만 저장되어야 함
        verify(storeDetailRepository, times(1)).saveAll(argThat(list -> {
            List<StoreDetail> details = (List<StoreDetail>) list;
            return details.size() == 1 && details.get(0).getStoreAppId().equals("200");
        }));
    }

    @Test
    @DisplayName("API 호출 중 에러 발생 시 로그 남기고 루프 탈출")
    void syncAppListApiErrorStopLoop() {
        // Given
        given(storeDetailRepository.findAllAppIdsByStore(any())).willReturn(Collections.emptySet());

        // 첫 호출부터 에러 발생
        given(collector.collectAppList(null)).willThrow(new RestClientException("Connection Timeout"));

        // When
        steamAppSyncService.syncAppList();

        // Then
        // 저장이 한 번도 호출되면 안 됨
        then(gameRepository).shouldHaveNoInteractions();
        then(transactionTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("API 응답 데이터가 DB에 있다면 저장하지 X")
    void syncAppListAllExistingNoSave() {
        // Given
        given(storeDetailRepository.findAllAppIdsByStore(any())).willReturn(Set.of("100", "200"));

        List<App> apps = List.of(
                new App(100L, "Game 1"),
                new App(200L, "Game 2")
        );
        Response responseBody = new Response(apps, false, 200L);
        SteamAppListResponse responseWrapper = new SteamAppListResponse(responseBody);

        given(collector.collectAppList(null)).willReturn(responseWrapper);

        // When
        steamAppSyncService.syncAppList();

        // Then
        // 필터링 결과 newApps가 비어있으므로 트랜잭션을 실행하면 안 됨
        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(gameRepository, never()).saveAll(any());
    }
}
