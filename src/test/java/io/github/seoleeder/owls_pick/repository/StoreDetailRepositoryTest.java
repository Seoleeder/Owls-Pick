package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.config.TestQueryDSLConfig;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.Review;
import io.github.seoleeder.owls_pick.entity.game.ReviewStat;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.support.AbstractContainerBaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 빈만 로드
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 교체 방지 (Testcontainers 사용)
@Import(TestQueryDSLConfig.class)
class StoreDetailRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private StoreDetailRepository storeDetailRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ReviewStatRepository reviewStatRepository;

    @Test
    @DisplayName("특정 스토어(STEAM)의 스토어 내 앱 ID 조회")
    void findAllAppIdsByStoreTest() {
        // Given
        Game game1 = gameRepository.save(Game.builder().title("Game 1").build());
        Game game2 = gameRepository.save(Game.builder().title("Game 2").build());

        storeDetailRepository.save(StoreDetail.builder().game(game1).storeName(StoreName.STEAM).storeAppId("1001").build());
        storeDetailRepository.save(StoreDetail.builder().game(game2).storeName(StoreName.STEAM).storeAppId("1002").build());

        // 다른 상점 (EPIC) - 조회되면 안 됨
        storeDetailRepository.save(StoreDetail.builder().game(game1).storeName(StoreName.EPIC_GAMES_STORE).storeAppId("EPIC_1").build());

        // When
        Set<String> appIds = storeDetailRepository.findAllAppIdsByStore(StoreName.STEAM);

        // Then
        assertThat(appIds).hasSize(2);
        assertThat(appIds).contains("1001", "1002");
        assertThat(appIds).doesNotContain("EPIC_1");
    }

    @Test
    @DisplayName("리뷰 통계 데이터가 없는(NULL) 게임만 조회")
    void findGamesWithNoReviewsTest() {
        // Given
        Game game1 = gameRepository.save(Game.builder().title("Reviewed Game").build());
        Game game2 = gameRepository.save(Game.builder().title("No Review Game").build());

        // StoreDetail 생성
        storeDetailRepository.save(StoreDetail.builder().game(game1).storeAppId("TEST_APP_ID_1").storeName(StoreName.STEAM).build());
        storeDetailRepository.save(StoreDetail.builder().game(game2).storeAppId("TEST_APP_ID_2").storeName(StoreName.STEAM).build());

        // game1은 리뷰 테이블에 데이터를 넣어줌
        reviewStatRepository.save(ReviewStat.builder()
                .game(game1) // 게임과 연관관계 매핑
                .reviewScore(80)
                .totalReview(150)
                .totalPositive(120)
                .totalNegative(30)
                .updatedAt(LocalDateTime.now())
                .build());

        // game2는 리뷰 데이터를 넣지 않음 (NULL 상태)

        // When
        List<StoreDetail> result = storeDetailRepository.findGamesWithNoReviews(StoreName.STEAM, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGame().getTitle()).isEqualTo("No Review Game");
    }

    @Test
    @DisplayName("리뷰 업데이트가 필요한 모든 스팀 게임 ID 조회")
    void findGamesNeedingReviewUpdateTest() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldDate = now.minusDays(30); // 30일 전 (업데이트 필요)
        LocalDateTime recentDate = now.minusDays(1); // 1일 전 (최신)

        Game oldGame = gameRepository.save(Game.builder().title("Old Review Game").build());
        Game newGame = gameRepository.save(Game.builder().title("Recent Review Game").build());

        storeDetailRepository.save(StoreDetail.builder().game(oldGame).storeAppId("01234").storeName(StoreName.STEAM).build());
        storeDetailRepository.save(StoreDetail.builder().game(newGame).storeAppId("56789").storeName(StoreName.STEAM).build());

        // 리뷰 통계 데이터 생성
        reviewStatRepository.save(ReviewStat.builder().game(oldGame).reviewScore(85)
                .totalReview(3000).totalPositive(2910). totalNegative(90).updatedAt(oldDate).build()); // 10일 전
        reviewStatRepository.save(ReviewStat.builder().game(newGame).reviewScore(69)
                .totalReview(156).totalPositive(106).totalNegative(50).updatedAt(recentDate).build()); // 1일 전

        // When
        List<StoreDetail> result = storeDetailRepository.findGamesNeedingReviewUpdate(StoreName.STEAM, 1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGame().getTitle()).isEqualTo("Old Review Game");
    }

    @Test
    @DisplayName("AppID 리스트로 스토어 상세 정보 조회 (IN절 검증)")
    void findByStoreNameAndStoreAppIdInTest() {
        // Given
        Game game1 = gameRepository.save(Game.builder().title("Target 1").build());
        Game game2 = gameRepository.save(Game.builder().title("Target 2").build());
        Game game3 = gameRepository.save(Game.builder().title("Not Target").build());

        storeDetailRepository.save(StoreDetail.builder().game(game1).storeName(StoreName.STEAM).storeAppId("ID_1").build());
        storeDetailRepository.save(StoreDetail.builder().game(game2).storeName(StoreName.STEAM).storeAppId("ID_2").build());
        storeDetailRepository.save(StoreDetail.builder().game(game3).storeName(StoreName.STEAM).storeAppId("ID_3").build());

        List<String> targetIds = List.of("ID_1", "ID_2");

        // When
        List<StoreDetail> result = storeDetailRepository.findByStoreNameAndStoreAppIdIn(StoreName.STEAM, targetIds);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(StoreDetail::getStoreAppId)
                .containsExactlyInAnyOrder("ID_1", "ID_2");
    }

    @Test
    @DisplayName("ITAD ID가 없는 (그래서 수집 후 저장해야 할) 게임의 스팀 App ID 조회")
    void findDetailsWithMissingItadIdTest() {
        // Given
        // ITAD ID가 있는 게임
        Game linkedGame = gameRepository.save(Game.builder().title("Linked").itadId("ITAD_123").build());
        storeDetailRepository.save(StoreDetail.builder().game(linkedGame).storeName(StoreName.STEAM).build());

        // ITAD ID가 없는 게임 (Target)
        Game missingGame = gameRepository.save(Game.builder().title("Missing").itadId(null).build());
        storeDetailRepository.save(StoreDetail.builder().game(missingGame).storeName(StoreName.STEAM).build());

        // When
        List<StoreDetail> result = storeDetailRepository.findDetailsWithMissingItadId(StoreName.STEAM, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGame().getTitle()).isEqualTo("Missing");
    }

    @Test
    @DisplayName("스토어 이름과 스토어 내 게임 ID로 스토어 상세정보 조회")
    void findDetailsByStoreAndGameIdsTest() {
        // Given
        Game g1 = gameRepository.save(Game.builder().title("G1").build());
        Game g2 = gameRepository.save(Game.builder().title("G2").build());
        Game g3 = gameRepository.save(Game.builder().title("G3").build());

        storeDetailRepository.save(StoreDetail.builder().game(g1).storeName(StoreName.STEAM).build());
        storeDetailRepository.save(StoreDetail.builder().game(g2).storeName(StoreName.STEAM).build());
        storeDetailRepository.save(StoreDetail.builder().game(g3).storeName(StoreName.EPIC_GAMES_STORE).build()); // 상점 다름

        // When
        List<StoreDetail> result = storeDetailRepository.findDetailsByStoreAndGameIds(StoreName.STEAM, List.of(g1.getId(), g2.getId()));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(sd -> sd.getGame().getTitle()).contains("G1", "G2");
    }
}