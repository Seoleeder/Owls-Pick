package io.github.seoleeder.owls_pick.service.client.itad;

import io.github.seoleeder.owls_pick.client.ITAD.ITADDataCollector;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse.Deal;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse.Deal.Price;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse.Deal.OriginalPrice;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse.Deal.StoreLow;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse.Deal.Shop;
import io.github.seoleeder.owls_pick.common.config.properties.ItadProperties;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ITADSyncServiceTest {

    private ITADSyncService itadSyncService;

    @Mock private ITADDataCollector collector;
    @Mock private GameRepository gameRepository;
    @Mock private StoreDetailRepository storeDetailRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        // 🦅 ITAD 설정 객체 생성 (batchSize 포함)
        ItadProperties props = new ItadProperties("test-key", "api.test.com", 100);

        itadSyncService = new ITADSyncService(
                collector,
                gameRepository,
                storeDetailRepository,
                transactionTemplate,
                props
        );
    }

    @Test
    @DisplayName("스팀 ID로 ITAD ID 조회 -> 게임 엔티티에 업데이트")
    void syncMissingItadIds_Success() {
        // Given
        Game game = Game.builder().id(1L).title("No ITAD ID Game").build();
        StoreDetail detail = StoreDetail.builder().game(game).storeAppId("STEAM_123").build();

        // 조회 (1회차: 있음 / 2회차: 없음)
        given(storeDetailRepository.findDetailsWithMissingItadId(any(), anyInt()))
                .willReturn(List.of(detail))
                .willReturn(Collections.emptyList());

        // API 호출
        given(collector.collectItadIds(List.of("STEAM_123")))
                .willReturn(Map.of("STEAM_123", "ITAD_456"));

        // 트랜잭션 모킹
        doAnswer(inv -> 1).when(transactionTemplate).execute(any());

        // When
        itadSyncService.syncMissingItadIds();

        // Then
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    @DisplayName("가격 변동이 있는 경우에만 가격 정보 저장")
    void syncPrices_UpdateOnlyIfChanged() {
        // Given
        Game game = Game.builder().id(1L).itadId("ITAD_1").title("Game 1").build();
        given(gameRepository.findByItadIdIsNotNull()).willReturn(List.of(game));

        // API 응답 생성
        // 현재가 1000원, 정가 2000원, 최저가 500원
        Deal deal = new Deal(
                new Shop("61", "Steam"),         // shop
                new Price(1000),                 // currentPrice (Integer)
                new OriginalPrice(2000),         // originalPrice
                new StoreLow(500),               // storelow
                50,                              // cut
                LocalDateTime.now().plusDays(1), // expiry
                "http://steam.com"               // url
        );
        ITADPriceResponse priceRes = new ITADPriceResponse("ITAD_1", List.of(deal));

        given(collector.collectPrices(anyList())).willReturn(List.of(priceRes));

        // 트랜잭션 실행 모킹
        doAnswer(inv -> {
            Consumer<TransactionStatus> callback = inv.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 기존 DB 상태 (현재가 2000원 != DB 저장된 가격 1000원)
        StoreDetail existingDetail = StoreDetail.builder()
                .game(game)
                .storeName(StoreName.STEAM)
                .discountPrice(2000) // 값 다름
                .build();

        given(storeDetailRepository.findByGameAndStoreName(any(), eq(StoreName.STEAM)))
                .willReturn(Optional.of(existingDetail));

        // When
        itadSyncService.syncPrices();

        // Then
        // 변경사항이 있으므로 saveAll이 호출되어야 함 (값 1000원 확인)
        verify(storeDetailRepository).saveAll(argThat(list -> {
            List<StoreDetail> details = (List<StoreDetail>) list;
            return details.size() == 1 && details.get(0).getDiscountPrice() == 1000;
        }));
    }

    @Test
    @DisplayName("가격 변동이 없으면 저장 X")
    void syncPrices_SkipIfSame() {
        // Given
        Game game = Game.builder().id(1L).itadId("ITAD_1").build();
        given(gameRepository.findByItadIdIsNotNull()).willReturn(List.of(game));

        // API 응답 (정가 1000원, 할인 없음 0%)
        Deal deal = new Deal(
                new Shop("61", "Steam"),
                new Price(1000),                 // Current Price
                new OriginalPrice(1000),         // Original Price
                null,                            // StoreLow (null)
                0,                               // Cut (0%)
                LocalDateTime.now().plusDays(1), // Expiry (할인 없어서 무시)
                "http://steam.com"               // URL
        );
        ITADPriceResponse priceRes = new ITADPriceResponse("ITAD_1", List.of(deal));

        given(collector.collectPrices(anyList())).willReturn(List.of(priceRes));

        // 트랜잭션 템플릿 모킹
        doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 2. [핵심] DB 데이터 상태 정의
        // 서비스 로직상 할인율(Cut)이 0이면 -> discountPrice와 expiryDate는 'null' 이어야 같다고 판단함!
        StoreDetail sameDetail = StoreDetail.builder()
                .id(1L)
                .game(game)
                .storeName(StoreName.STEAM)
                .originalPrice(1000)
                .discountPrice(null)
                .discountRate(0)
                .expiryDate(null)
                .historicalLow(null)
                .url("http://steam.com")
                .build();

        given(storeDetailRepository.findByGameAndStoreName(any(), any()))
                .willReturn(Optional.of(sameDetail));

        // When
        itadSyncService.syncPrices();

        // Then
        // 데이터가 완벽히 같으므로 saveAll은 호출되지 않아야 함
        verify(storeDetailRepository, never()).saveAll(anyList());
    }
}