package io.github.seoleeder.owls_pick.service.client.steam;

import io.github.seoleeder.owls_pick.client.steamweb.SteamDataCollector;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewDetailResponse.SteamReviewDetail;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewStatsResponse.SteamReviewStats;
import io.github.seoleeder.owls_pick.common.config.properties.SteamProperties;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.repository.ReviewRepository;
import io.github.seoleeder.owls_pick.repository.ReviewStatRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamReviewSyncServiceTest {

    private SteamReviewSyncService steamReviewSyncService;

    @Mock private SteamDataCollector collector;
    @Mock private StoreDetailRepository storeDetailRepository;
    @Mock private ReviewStatRepository reviewStatRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private TransactionTemplate transactionTemplate;

    /**
     * 테스트 메서드 실행 전에 실행
     * Mock 객체 + properties 생성자 주입 (수동)
     * */
    @BeforeEach
    void setUp() {
        SteamProperties props = new SteamProperties(
                null,
                null,
                new SteamProperties.Sync(20), // threadPoolSize
                new SteamProperties.Review(5, 200, 200), // minVotesUp, init, maintenance
                null
        );

        steamReviewSyncService = new SteamReviewSyncService(
                collector,
                storeDetailRepository,
                reviewStatRepository,
                reviewRepository,
                transactionTemplate,
                props
        );
    }

    @Test
    @DisplayName("리뷰가 없는 게임을 조회하여 API 호출 후 저장")
    void initAllReviews_Success() {
        // Given
        // 1. 대상 게임 데이터 준비
        Game game = Game.builder().id(1L).title("Elden Ring").build();
        StoreDetail detail = StoreDetail.builder().game(game).storeAppId("12345").build();

        // 2. 루프 제어 모킹 (첫 번째 호출엔 데이터 있음 -> 두 번째 호출엔 빈 리스트로 루프 종료)
        given(storeDetailRepository.findGamesWithNoReviews(any(), anyInt()))
                .willReturn(List.of(detail))
                .willReturn(Collections.emptyList());

        // 3. API 응답 데이터 준비
        SteamReviewStats stats = new SteamReviewStats(95, "Overwhelmingly Positive", 1100, 1050, 50);
        SteamReviewDetail reviewDetail = new SteamReviewDetail(100L, new SteamReviewDetail.Author(7), new BigDecimal(6.7), "Nice Game!", 10, true, Instant.now().getEpochSecond());
        SteamReviewResponse response = new SteamReviewResponse(stats, List.of(reviewDetail));

        given(collector.collectRefinedReviews(12345L, 5)).willReturn(response);

        // 4. TransactionTemplate 모킹 (비동기 스레드 안에서도 동작)
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null); // saveReviewData 실행
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // 5. 기존 통계/리뷰 존재 여부 (신규 저장 시나리오)
        given(reviewStatRepository.findById(1L)).willReturn(Optional.empty());
        given(reviewRepository.existsByGameIdAndRecommendationId(1L, 100L)).willReturn(false);

        // When
        steamReviewSyncService.initAllReviews();

        // Then
        // 1. 리뷰 통계(ReviewStat) 저장 호출 검증
        verify(reviewStatRepository, times(1)).save(any(ReviewStat.class));

        // 2. 리뷰 상세(Review) 저장 호출 검증
        verify(reviewRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("이미 통계가 존재하면 updateStats를 호출하고, 중복 리뷰는 건너뛰어야 한다")
    void initAllReviews_UpdateExisting() {
        // Given
        Game game = Game.builder().id(1L).title("Dark Souls").build();
        StoreDetail detail = StoreDetail.builder().game(game).storeAppId("999").build();

        given(storeDetailRepository.findGamesWithNoReviews(any(), anyInt()))
                .willReturn(List.of(detail))
                .willReturn(Collections.emptyList());

        // API 응답 (리뷰 2개: ID "A"(중복), ID "B"(신규))
        SteamReviewStats stats = new SteamReviewStats(90, "Very Positive", 550, 500, 50);
        SteamReviewDetail reviewA = new SteamReviewDetail(170L, new SteamReviewDetail.Author(7), new BigDecimal(1.2), "Good!", 5, true, Instant.now().getEpochSecond());
        SteamReviewDetail reviewB = new SteamReviewDetail(180L, new SteamReviewDetail.Author(8), new BigDecimal(3.4), "Best Driver!", 8, true, Instant.now().getEpochSecond());

        given(collector.collectRefinedReviews(999L, 5))
                .willReturn(new SteamReviewResponse(stats, List.of(reviewA, reviewB)));

        // 트랜잭션 실행 강제
        doAnswer(inv -> { ((Consumer<TransactionStatus>)inv.getArgument(0)).accept(null); return null; })
                .when(transactionTemplate).executeWithoutResult(any());

        // Mock 객체(ReviewStat) 준비 -> updateStats 호출 여부 확인용
        ReviewStat mockStat = mock(ReviewStat.class);
        given(reviewStatRepository.findById(1L)).willReturn(Optional.of(mockStat));

        // 리뷰 중복 체크 ("A"는 있고, "B"는 없음)
        given(reviewRepository.existsByGameIdAndRecommendationId(1L, 170L)).willReturn(true);
        given(reviewRepository.existsByGameIdAndRecommendationId(1L, 180L)).willReturn(false);

        // When
        steamReviewSyncService.initAllReviews();

        // Then
        // 1. 기존 통계 객체의 updateStats 메서드가 호출되었는지 확인
        verify(mockStat).updateStats(90, "Very Positive", 550, 500, 50);

        // 2. 중복이 아닌 리뷰("B")만 담아서 saveAll이 호출되었는지 확인
        verify(reviewRepository).saveAll(argThat(list -> ((Collection<?>) list).size() == 1));
    }
}