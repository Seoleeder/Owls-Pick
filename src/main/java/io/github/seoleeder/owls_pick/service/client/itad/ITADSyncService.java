package io.github.seoleeder.owls_pick.service.client.itad;

import io.github.seoleeder.owls_pick.client.itad.ITADDataCollector;
import io.github.seoleeder.owls_pick.client.itad.ITADStore;
import io.github.seoleeder.owls_pick.client.itad.dto.ITADPriceResponse;
import io.github.seoleeder.owls_pick.global.config.properties.ItadProperties;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ITADSyncService {

    private final ITADDataCollector collector;
    private final GameRepository gameRepository;
    private final StoreDetailRepository storeDetailRepository;

    private final TransactionTemplate transactionTemplate;

    private final ItadProperties props;

    private static final String NOT_FOUND = "NONE";
    private static final String MAIN_GAME_TYPE = "Main Game";

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
        int totalAttempted = 0;
        Long lastId = 0L; //커서 기반 페이징

        while (true) {
            try {
//                // Steam AppID만 있고 ITAD ID가 없는 게임 조회 (batch : 200)
//                List<StoreDetail> targetDetails = storeDetailRepository.findDetailsWithMissingItadId(
//                        StoreName.STEAM, props.batchSize());

                List<StoreDetail> targetDetails = storeDetailRepository.findValidGamesMissingItadId(
                        StoreName.STEAM,
                        lastId,
                        props.batchSize()
                );

                if (targetDetails.isEmpty()) {
                    log.info("No more games missing ITAD IDs.");
                    break;
                }

                // 마지막 요소의 ID로 커서 갱신
                lastId = targetDetails.get(targetDetails.size() - 1).getId();

                // Steam AppID 추출 및 매핑 준비
                Map<String, Game> steamIdToGameMap = targetDetails.stream()
                        .collect(Collectors.toMap(
                                StoreDetail::getStoreAppId,
                                StoreDetail::getGame,
                                (existing, replacement) -> existing
                        ));

                //SteamID 목록 -> ITAD UUID 목록 변환
                List<String> steamIds = new ArrayList<>(steamIdToGameMap.keySet());

                log.info(">> ITAD API Request: Fetching UUIDs for {} games...", steamIds.size());
                long start = System.currentTimeMillis();

                Map<String, String> resultMap = collector.collectItadIds(steamIds);

                long duration = System.currentTimeMillis() - start;
                log.info("<< ITAD API Response: Received {} matches. (Duration: {}ms)", resultMap.size(), duration);

                // DB 업데이트 (Transactional Helper 호출)
                Integer batchUpdated = transactionTemplate.execute(status -> updateItadIds(steamIdToGameMap, resultMap));

                if(batchUpdated != null) {
                    totalUpdated += batchUpdated;
                }

                totalAttempted += targetDetails.size();

                // 5. 진행 상황 로그 (initialUpdated 없이도 totalUpdated로 충분합니다)
                log.info("Progress: Mapped {} new IDs in this batch. (Cumulative: {} / Attempted: {})",
                        batchUpdated, totalUpdated, totalAttempted);

                sleep(100);

            } catch (RestClientException e) {
                log.warn("ITAD API Error during ID Sync: {}", e.getMessage());
                sleep(300);
            } catch (Exception e) {
                log.error("Error occurred during ITAD ID Sync batch.", e);
                sleep(300);
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
        int mappedCount = 0; // 스팀 ID과 ITAD ID 가 매칭된 횟수

        for (Map.Entry<String, Game> entry : gameMap.entrySet()) {
            String steamId = entry.getKey();
            Game game = entry.getValue();

            // ITAD API 결과 맵에서 UUID가 있는지 확인
            String itadUuid = itadIdMap.get(steamId);

            if (itadUuid != null) {
                // 매칭 성공: 실제 UUID 저장
                game.updateItadId(itadUuid);
                mappedCount++;
            } else {
                // 매칭 실패: "NONE"으로 마킹하여 다음 쿼리에서 제외되도록 함
                game.updateItadId("NONE");
            }

            gamesToSave.add(game);
        }

        // "NONE"으로 마킹된 게임을 포함하여 모두 DB에 반영
        if (!gamesToSave.isEmpty()) {
            gameRepository.saveAll(gamesToSave);
        }

        // 로그 출력을 위해 '실제로 매칭된 개수'만 반환
        return mappedCount;
    }

    /**
     * ITAD ID로 최신 가격 데이터 수집
     * 정가, 할인가, 할인율, 할인 만료 시각, 스토어 구매 URL
     * (주기적으로 실행되는 메인 로직)
     * */
    public void syncPrices() {
        log.info("Starting ITAD Price Sync...");

        // ITAD ID가 있는 모든 게임 조회
        List<Game> gamesWithItadId = gameRepository.findByItadIdIsNotNullAndItadIdNot(NOT_FOUND);

        if (gamesWithItadId.isEmpty()) {
            log.info("No games with ITAD IDs found. Run syncMissingItadIds() first.");
            return;
        }

        // 리스트를 배치 사이즈에 맞게 분할
        List<List<Game>> partitions = partitionList(gamesWithItadId, props.batchSize());
        int totalProcessed = 0;
        int totalBatches = partitions.size();

        for (int i = 0; i < totalBatches; i++) {
            List<Game> batchGames = partitions.get(i);
            try {

                log.info("Processing price batch {}/{} (Size: {})", i + 1, totalBatches, batchGames.size());
                // 배치 처리 실행 (트랜잭션 단위)
                processPriceBatch(batchGames);
                totalProcessed += batchGames.size();

                sleep(100);

            } catch (Exception e) {
                // 한 배치가 실패해도 로그만 찍고 다음 배치를 계속 진행
                log.error("Failed to process price batch (Size: {}). Skipping to next batch.", batchGames.size(), e);

                sleep(300);
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
                    {
                        try {
                            savePricesInternal(games, prices);
                        } catch (Exception e) {
                            log.error("[CRITICAL-INNER] Error inside Transaction!", e);
                            throw e; // 트랜잭션 롤백을 위해 다시 던짐
                        }
                    }
//                    savePricesInternal(games, finalPrices)
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
        // 동일한 Itad Id를 가진 모든 게임을 묶음
        Map<String, List<Game>> itadIdToGamesMap = games.stream()
                .collect(Collectors.groupingBy(Game::getItadId));

        log.info("[ENTRY] Games in Map: {}, Prices from API: {}", itadIdToGamesMap.size(), prices.size());

        //저장할 스토어 상세 정보 리스트
        List<StoreDetail> detailsToSave = new ArrayList<>();

        log.info("[DEBUG] Received prices from ITAD: {} items", prices.size());

        for (ITADPriceResponse priceResponse : prices) {
            List<Game> matchedGames = itadIdToGamesMap.get(priceResponse.id()); // Itad Id로 게임 찾기
            if (matchedGames == null || priceResponse.deals() == null){
                log.warn("[DEBUG] No matched games in DB for ITAD ID: {}", priceResponse.id());
                continue;
            }

            // 동일한 ID를 가진 게임들 중  본편만 추출
            Game game = findCanonicalGame(matchedGames);

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
                        .orElseGet(() -> {
                                return StoreDetail.builder()
                                .game(game)
                                .storeName(storeName)
                                .storeAppId(null)
                                .build();
                        });

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
                Integer historicalLow = (deal.storeLow() != null) ? deal.storeLow().amountInt() : null; // 역대 최저가
                Integer cut = (deal.cut() != null) ? deal.cut() : 0; // 할인율
                OffsetDateTime expiry = deal.expiryDate(); // 할인 만료 시각

                LocalDateTime expiryKst = null;

                if (expiry != null) {
                    expiryKst = expiry
                            .atZoneSameInstant(ZoneId.of("Asia/Seoul")) // 타임존을 서울로 변경
                            .toLocalDateTime();
                }


                // 변경 감지 (Skip 최적화)
                boolean isSame = isSamePriceInfo(storeDetail, currentPrice, originalPrice, cut, historicalLow, expiryKst, urlToSave);

                if (!isSame) {
                    log.info("[TARGET-ALIVE] Found changes for: {} (Store: {})", game.getTitle(), storeName);
                    storeDetail.updatePriceInfo(currentPrice, originalPrice, historicalLow, cut, expiryKst, urlToSave);
                    detailsToSave.add(storeDetail);
                }

                // 엔티티 업데이트
                storeDetail.updatePriceInfo(
                        currentPrice,
                        originalPrice,
                        historicalLow,
                        cut, expiryKst,
                        urlToSave
                );
                detailsToSave.add(storeDetail);
            }
        }

        log.info("[DEBUG] Total StoreDetails to save: {}", detailsToSave.size());

        // 변경된 데이터만 일괄 저장
        if (!detailsToSave.isEmpty()) {
            storeDetailRepository.saveAll(detailsToSave);
            log.info("[DEBUG] Successfully saved {} details to DB", detailsToSave.size());
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

        //DB는 비어있고, 새로운 값이 들어온 경우 (초기 수집)
        if (detail.getOriginalPrice() == null && (reg != null || cur != null)) return false;

        // 정가 비교
        if (!Objects.equals(detail.getOriginalPrice(), reg)) return false;

        // 할인율 비교
        if(!Objects.equals(detail.getDiscountRate(), cut)) return false;

        // 할인가(discountPrice) 비교
        // 정책: 할인 중(cut > 0)이면 현재가가 할인가, 아니면 Null
        Integer dbDiscountPrice = detail.getDiscountPrice();
        Integer targetDiscountPrice = (cut > 0) ? cur : null;
        if (!Objects.equals(detail.getDiscountRate(), (cut == null ? 0 : cut))) return false;
        if (!Objects.equals(dbDiscountPrice, targetDiscountPrice)) return false;

        // 역대 최저가 비교
        if (!Objects.equals(detail.getHistoricalLow(), low)) return false;


        // 만료일 비교 (할인 중일 때만 유효)
        if (!Objects.equals(detail.getExpiryDate(), expiry)) return false;
        if (!Objects.equals(detail.getUrl(), url)) return false;

        // 기타 (할인율, URL)
        return true;
    }

    /**
     * 동일한 ITAD ID를 가진 여러 게임 중 본편만 반환
     */
    private Game findCanonicalGame(List<Game> games) {
        // 단일 매칭이면 바로 반환
        if (games.size() == 1) {
            return games.get(0);
        }

        // 게임 타입이 Main Game인 게임들만 필터링
        List<Game> mainGames = games.stream()
                .filter(g -> g.getType() != null && MAIN_GAME_TYPE.equals(g.getType().toString()))
                .toList();

        // Main Game이 존재하면 그것들로, 존재하지 않는다면 전체 게임 리스트를 후보군으로 선정
        List<Game> candidates = mainGames.isEmpty() ? games : mainGames;

        // 후보군 중에서 출시일이 가장 오래된 게임 선택
        return candidates.stream()
                .min(Comparator.comparing(Game::getFirstRelease, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(candidates.get(0));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
