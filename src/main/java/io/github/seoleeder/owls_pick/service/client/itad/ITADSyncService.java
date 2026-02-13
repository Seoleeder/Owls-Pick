package io.github.seoleeder.owls_pick.service.client.itad;

import io.github.seoleeder.owls_pick.client.ITAD.ITADDataCollector;
import io.github.seoleeder.owls_pick.client.ITAD.ITADStore;
import io.github.seoleeder.owls_pick.client.ITAD.dto.ITADPriceResponse;
import io.github.seoleeder.owls_pick.common.config.properties.ItadProperties;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ITADSyncService {

    private final ITADDataCollector collector;
    private final GameRepository gameRepository;
    private final StoreDetailRepository storeDetailRepository;

    private final TransactionTemplate transactionTemplate;

    private final ItadProperties props;

    public ITADSyncService(ITADDataCollector collector, GameRepository gameRepository, StoreDetailRepository storeDetailRepository, TransactionTemplate transactionTemplate, ItadProperties props) {
        this.collector = collector;
        this.gameRepository = gameRepository;
        this.storeDetailRepository = storeDetailRepository;
        this.transactionTemplate = transactionTemplate;
        this.props = props;
    }


    /**
     * Steam ID로 ITAD UUID 조회 & 저장
     * (ITAD ID가 없는 게임들에 대해 수행)
     */
    public void syncMissingItadIds() {
        log.info("Starting ITAD ID Sync (Filling missing IDs)...");
        int totalUpdated = 0;

        while (true) {
            try {
                // Steam AppID만 있고 ITAD ID가 없는 게임 조회 (batch : 200)
                List<StoreDetail> targetDetails = storeDetailRepository.findDetailsWithMissingItadId(
                        StoreName.STEAM,
                        props.batchSize()
                );

                if (targetDetails.isEmpty()) {
                    log.info("No more games missing ITAD IDs.");
                    break;
                }

                // Steam AppID 추출 및 매핑 준비
                Map<String, Game> steamIdToGameMap = targetDetails.stream()
                        .collect(Collectors.toMap(
                                StoreDetail::getStoreAppId,
                                StoreDetail::getGame,
                                (existing, replacement) -> existing
                        ));

                //SteamID 목록 -> ITAD UUID 목록 변환
                List<String> steamIds = new ArrayList<>(steamIdToGameMap.keySet());
                Map<String, String> resultMap = collector.collectItadIds(steamIds);

                // 결과로 받은 itadId가 비어있으면 break
                if (resultMap.isEmpty()) {
                    log.warn("ITAD API returned no matches for current batch. Stopping loop to prevent infinite cycle.");
                    break;
                }

                // DB 업데이트 (Transactional Helper 호출)
                Integer batchUpdated = transactionTemplate.execute(status -> updateItadIds(steamIdToGameMap, resultMap));

                if(batchUpdated != null) {
                    totalUpdated += batchUpdated;
                }

                sleep(500);

            } catch (RestClientException e) {
                log.warn("ITAD API Error during ID Sync: {}", e.getMessage());
                sleep(3000);
            } catch (Exception e) {
                log.error("Error occurred during ITAD ID Sync batch.", e);
                sleep(3000);
            }
        }

        log.info("ITAD ID Sync Completed. Total games updated: {}", totalUpdated);
    }

    /**
     * 두 맵을 받아서 매핑 후 Game 엔티티에 itadId 저장
     * @param gameMap [스팀 ID, Game]
     * @param itadIdMap [스팀 ID, ITAD ID]
     * */
    protected int updateItadIds(Map<String, Game> gameMap, Map<String, String> itadIdMap) {
        List<Game> gamesToSave = new ArrayList<>();

        for (Map.Entry<String, String> entry : itadIdMap.entrySet()) {
            String steamId = entry.getKey();
            String itadUuid = entry.getValue();

            Game game = gameMap.get(steamId);
            if (game != null) {
                game.updateItadId(itadUuid);
                gamesToSave.add(game);
            }
        }

        if (!gamesToSave.isEmpty()) {
            gameRepository.saveAll(gamesToSave);
        }

        return gamesToSave.size();
    }

    /**
     * ITAD ID로 최신 가격 데이터 수집
     * 정가, 할인가, 할인율, 할인 만료 시각, 스토어 구매 URL
     * (주기적으로 실행되는 메인 로직)
     * */
    public void syncPrices() {
        log.info("Starting ITAD Price Sync...");

        // ITAD ID가 있는 모든 게임 조회
        List<Game> gamesWithItadId = gameRepository.findByItadIdIsNotNull();

        if (gamesWithItadId.isEmpty()) {
            log.info("No games with ITAD IDs found. Run syncMissingItadIds() first.");
            return;
        }

        // 리스트를 배치 사이즈에 맞게 분할
        List<List<Game>> partitions = partitionList(gamesWithItadId, props.batchSize());
        int totalProcessed = 0;

        for (List<Game> batchGames : partitions) {
            try {
                // 배치 처리 실행 (트랜잭션 단위)
                processPriceBatch(batchGames);
                totalProcessed += batchGames.size();

                sleep(500);

            } catch (Exception e) {
                // 한 배치가 실패해도 로그만 찍고 다음 배치를 계속 진행
                log.error("Failed to process price batch (Size: {}). Skipping to next batch.", batchGames.size(), e);

                sleep(2000);
            }
        }

        log.info("ITAD Price Sync Completed. Processed {} games.", totalProcessed);
    }

    protected void processPriceBatch(List<Game> games) {

        // 가격 API 호출 & 실시간 데이터 수집
        List<ITADPriceResponse> prices;
        try {
            // 요청용 ITAD ID 리스트 추출
            List<String> itadIds = games.stream().map(Game::getItadId).toList();

            // Collector 호출 (가격 정보 수집)
            prices = collector.collectPrices(itadIds);
        } catch (RestClientException e) {
            log.warn("ITAD Price API Failed. Skipping batch. Error: {}", e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Unexpected Error during ITAD Price API call.", e);
            return;
        }

        if (prices == null || prices.isEmpty()) return;

        //가격 정보 DB 저장
        try {
            // 람다용 final 변수
            List<ITADPriceResponse> finalPrices = prices;

            transactionTemplate.executeWithoutResult(status ->
                    savePricesInternal(games, finalPrices)
            );

        } catch (Exception e) {
            log.error("Failed to save Price Batch.", e);
        }

    }

    /**
     * 게임 가격 정보 저장 로직 (StoreDetail Update)
     * @param games 가격 정보를 저장할 게임 리스트
     * @param prices 게임의 스토어별 가격 정보
     */
    private void savePricesInternal(List<Game> games, List<ITADPriceResponse> prices) {
        // 응답 매핑용 Map 생성 (Itad Id -> Game)
        Map<String, Game> itadIdToGameMap = games.stream()
                .collect(Collectors.toMap(Game::getItadId, Function.identity()));

        //저장할 스토어 상세 정보 리스트
        List<StoreDetail> detailsToSave = new ArrayList<>();


        for (ITADPriceResponse priceResponse : prices) {
            Game game = itadIdToGameMap.get(priceResponse.id()); // Itad Id로 게임 찾기
            if (game == null || priceResponse.deals() == null) continue;

            // 딜 리스트 순회
            for (ITADPriceResponse.Deal deal : priceResponse.deals()) {
                if (deal.shop() == null || deal.currentPrice() == null) continue;

                // 스토어 식별 (ITAD ID -> Enum)
                ITADStore itadStore = ITADStore.fromId(Integer.parseInt(deal.shop().id()));
                if (itadStore == null || itadStore.getStoreName() == null) continue;

                //DB에 저장될 스토어 이름
                StoreName storeName = itadStore.getStoreName();

                // Upsert 조회 (기존 데이터 있는지 확인하고 없으면 빌드)
                StoreDetail storeDetail = storeDetailRepository.findByGameAndStoreName(game, storeName)
                        .orElseGet(() -> StoreDetail.builder()
                                .game(game)
                                .storeName(storeName)
                                .storeAppId(null)
                                .build());

                // URL 정보
                // - Steam: 기존 URL이 있으면 유지
                // - Others: ITAD에서 취급하는 URL 사용
                String urlToSave;
                if (storeName == StoreName.STEAM) {
                    urlToSave = (storeDetail.getUrl() != null && !storeDetail.getUrl().isBlank())
                            ? storeDetail.getUrl()
                            : deal.url();
                } else {
                    urlToSave = deal.url();
                }

                //게임 가격 정보 추출
                Integer currentPrice = deal.currentPrice().amountInt(); // 현재가 (할인 시에는 할인가)
                Integer originalPrice = (deal.originalPrice() != null) ? deal.originalPrice().amountInt() : currentPrice; // 정가
                Integer historicalLow = (deal.storelow() != null) ? deal.storelow().amountInt() : null; // 역대 최저가
                Integer cut = (deal.cut() != null) ? deal.cut() : 0; // 할인율
                LocalDateTime expiry = deal.expiryDate(); // 할인 만료 시각

                // 변경 감지 (Skip 최적화)
                if (isSamePriceInfo(storeDetail, currentPrice, originalPrice, cut, historicalLow, expiry, urlToSave)) {
                    continue;
                }

                // 엔티티 업데이트
                storeDetail.updatePriceInfo(
                        currentPrice,
                        originalPrice,
                        historicalLow,
                        cut, expiry,
                        urlToSave
                );
                detailsToSave.add(storeDetail);
            }
        }

        // 변경된 데이터만 일괄 저장
        if (!detailsToSave.isEmpty()) {
            storeDetailRepository.saveAll(detailsToSave);
        }
    }

    // ---------------------------------------------------------------------------------
    // Helper Methods
    // ---------------------------------------------------------------------------------


    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * 변경 감지 로직 (Dirty Checking)
     * - DB의 값과 API에서 가져온 값이 '논리적으로' 같은지 비교
     */
    private boolean isSamePriceInfo(StoreDetail detail, Integer cur, Integer reg, Integer cut,
                                    Integer low, LocalDateTime expiry, String url) {
        if (detail.getId() == null) return false; // 신규 데이터는 무조건 저장

        // 1. 정가 비교
        if (!Objects.equals(detail.getOriginalPrice(), reg)) return false;

        // 2. 역대 최저가 비교
        if (!Objects.equals(detail.getHistoricalLow(), low)) return false;

        // 3. 할인가(discountPrice) 비교
        // 정책: 할인 중(cut > 0)이면 현재가가 할인가, 아니면 Null
        Integer targetDiscountPrice = (cut > 0) ? cur : null;
        if (!Objects.equals(detail.getDiscountPrice(), targetDiscountPrice)) return false;

        // 4. 만료일 비교 (할인 중일 때만 유효)
        LocalDateTime targetExpiry = (cut > 0) ? expiry : null;
        if (!Objects.equals(detail.getExpiryDate(), targetExpiry)) return false;

        // 5. 기타 (할인율, URL)
        return Objects.equals(detail.getDiscountRate(), cut) &&
                Objects.equals(detail.getUrl(), url);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
