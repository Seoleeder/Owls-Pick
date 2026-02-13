package io.github.seoleeder.owls_pick.service.client.igdb;

import io.github.seoleeder.owls_pick.client.IGDB.IGDBDataCollector;
import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameDetailResponse;
import io.github.seoleeder.owls_pick.client.IGDB.dto.IGDBGameSummaryResponse;
import io.github.seoleeder.owls_pick.common.util.TimestampUtils;
import io.github.seoleeder.owls_pick.entity.game.*;
import io.github.seoleeder.owls_pick.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IGDBSyncService {
    private final IGDBDataCollector collector;

    // Repositories
    private final GameRepository gameRepository;
    private final StoreDetailRepository storeDetailRepository;
    private final TagRepository tagRepository;
    private final ScreenshotRepository screenshotRepository;
    private final CompanyRepository companyRepository;
    private final GameCompanyRepository gameCompanyRepository;
    private final LanguageSupportRepository languageSupportRepository;

    private final TransactionTemplate transactionTemplate;

    /**
     * 초기 대량 수집 메서드 (ID 기준)
     * 조건에 맞는 게임 데이터 수집
     * ID = 0 부터 순차적으로 수집
     * */
    public void backfillAllGames() {
        log.info("Starting IGDB Full Backfill (ID based)...");

        //DB에서 가장 높은 IGDB ID를 조회해서 해당 위치부터 시작
        long lastId = gameRepository.findTopByOrderByIgdbIdDesc()
                .map(Game::getIgdbId)
                .orElse(0L);

        log.info("Resuming backfill from ID: {}", lastId);

        while (true) {
            try {
                // Collector 호출 (ID 기준 수집)
                List<IGDBGameSummaryResponse> summaries = collector.collectGameSummary(lastId);

                if (summaries.isEmpty()) {
                    log.info("Backfill Completed! Last processed ID: {}", lastId);
                    break;
                }

                // 게임 테이블 공통 배치 처리
                processBatch(summaries);

                // 커서 업데이트 (마지막 ID)
                lastId = summaries.getLast().igdbId();
                log.info("Processed batch up to ID: {}", lastId);

                // Rate Limit 방지 (IGDB 정책 준수 - 초당 4회 제한)
                sleep(250);

            } catch (RestClientException e) {
                log.warn("IGDB API Error at ID {}: {}", lastId, e.getMessage());
                sleep(5000);
            }
            catch (Exception e) {
                log.error("Backfill paused at ID: {}", lastId, e);
                sleep(5000); // 에러 시 잠시 대기 후 재시도
            }
        }
    }

    /**
     * IGDB 데이터 정기 업데이트
     * DB의 '최종 수정 시각' 이후에 변경된 데이터만 수집
     */
    public void syncUpdatedGames() {
        // IGDB 데이터 최종 수정 시각 조회 (Epoch time)
        long lastTimestamp = gameRepository.findMaxIgdbUpdatedAt()
                        .map(TimestampUtils::toEpoch)
                                .orElse(0L);

        log.info("Starting Update Sync (Timestamp based) from: {}", lastTimestamp);

        while (true) {
            try {
                // Collector 호출 (Timestamp 기준 수집 - 신규 및 수정 감지)
                List<IGDBGameSummaryResponse> summaries = collector.collectUpdatedGameSummary(lastTimestamp);

                if (summaries.isEmpty()) {
                    log.info("No more updates found.");
                    break;
                }

                // 게임 테이블 공통 배치 처리
                processBatch(summaries);

                // 커서 업데이트 (마지막 수정 시간)
                lastTimestamp = summaries.getLast().updatedAt();
                log.info("Synced batch up to timestamp: {}", lastTimestamp);

                sleep(250);

            }catch (RestClientException e) {
                log.warn("IGDB API Error at Timestamp {}: {}", lastTimestamp, e.getMessage());
                sleep(5000);
            }
            catch (Exception e) {
                log.error("Update Sync Failed", e);
                break;
            }
        }
    }

    /**
     * 500개 단위의 게임 주요 데이터를 저장하고, 상세 정보(Company, Language Support, Screenshot, Tag)까지 저장
     */

    protected void processBatch(List<IGDBGameSummaryResponse> summaries) {
        // Step 1: Summary 저장 (Game 테이블 + StoreDetail 연결)
        List<Game> savedGames;
        try {
            // 템플릿을 블록({})처럼 사용 -> 가독성 좋음
            savedGames = transactionTemplate.execute(status ->
                    upsertGamesSummaries(summaries)
            );
        } catch (Exception e) {
            log.error("Failed to save Game Summaries batch.", e);
            return; // 기본 정보 저장 실패하면 중단
        }

        if (savedGames == null || savedGames.isEmpty()) return;

        // Step 2: 상세 정보 수집을 위한 ID 추출
        List<Long> igdbIds = savedGames.stream().map(Game::getIgdbId).toList();

        // Step 3: 상세 정보 API 호출 (Collector 내부에서 Batch 처리됨)
        List<IGDBGameDetailResponse> details;
        try {
            details = collector.collectGameDetail(igdbIds);

        } catch (RestClientException e) {
            log.warn("IGDB API Error. Skipping details for this batch: {}", e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Unexpected error during API call.", e);
            return;
        }

        if (details == null || details.isEmpty()) return;

        // Step 4: 연관 테이블 저장 (Tag, Screenshot, Company, Language)
        try {
            List<Game> finalSavedGames = savedGames;
            List<IGDBGameDetailResponse> finalDetails = details;

            transactionTemplate.executeWithoutResult(status ->
                    syncDetails(finalSavedGames, finalDetails)
            );
        } catch (Exception e) {
            log.error("Failed to save Game Details batch.", e);
        }
    }



    /**
     * 게임 주요 데이터 저장 & 업데이트
     * 세부 구현: Summary -> StoreDetail 조회 (Steam ID와 IGDB ID 매핑) -> Game 엔티티 업데이트
     * */
    private List<Game> upsertGamesSummaries(List<IGDBGameSummaryResponse> summaries) {
        // IGDB 응답에서 Steam AppID 추출 및 맵핑 (Key: SteamAppID, Value: DTO)
        Map<String, IGDBGameSummaryResponse> steamIdToDtoMap = new HashMap<>();
        List<String> steamAppIds = new ArrayList<>();

        for (IGDBGameSummaryResponse dto : summaries) {
            String steamId = extractSteamId(dto);
            if (steamId != null) {
                steamAppIds.add(steamId);
                steamIdToDtoMap.put(steamId, dto);
            }
        }

        if (steamAppIds.isEmpty()) return List.of();

        // 스팀 ID로 이미 DB에 등록되어 있는 스팀 게임 조회
        List<StoreDetail> existingDetails = storeDetailRepository.findByStoreNameAndStoreAppIdIn(
                StoreDetail.StoreName.STEAM,
                steamAppIds
        );

        List<Game> gamesToUpdate = new ArrayList<>();

        // Game 엔티티 업데이트
        for (StoreDetail detail : existingDetails) {
            String steamId = detail.getStoreAppId();
            IGDBGameSummaryResponse dto = steamIdToDtoMap.get(steamId);

            if (dto != null) {
                Game game = detail.getGame(); // 기존 게임 엔티티

                if(game.getIgdbId() == null){
                    game.connectToIgdb(dto.igdbId());
                }

                // Epoch Time -> LocalDate, LocalDateTime
                // IGDB 기준 수정 시각, 게임 최초 출시일
                LocalDateTime igdbUpdatedAt = TimestampUtils.toLocalDateTime(dto.updatedAt());
                LocalDate firstRelease = TimestampUtils.toLocalDate(dto.first_release());

                // 한글화 타이틀, 심의 데이터 추출
                String localTitle = extractLocalization(dto); // Region ID (KR : 2)
                Ratings ratings = extractAgeRatings(dto.ageRatings()); // GRAC(국내 표준), ESRB(US 표준) 분리

                // 게임 커버 ID, 게임 타입, 게임 출시 상태 추출
                String coverId = (dto.cover() != null) ? dto.cover().imageId() : null;
                String typeName = (dto.type() != null) ? dto.type().type() : null;
                String statusName = (dto.gameStatus() != null) ? dto.gameStatus().status() : null;

                // 리스트에서 DB에 저장될 '이름' 데이터만 추출
                List<String> platforms = extractValues(dto.platforms(), IGDBGameSummaryResponse.Platform::name);
                List<String> modes = extractValues(dto.modes(), IGDBGameSummaryResponse.GameMode::name);
                List<String> perspectives = extractValues(dto.perspectives(), IGDBGameSummaryResponse.Perspective::name);

                game.updateFromSummary(// Title (필요 시 로직 수정)
                        localTitle,
                        typeName,
                        statusName,
                        platforms,
                        dto.description(),
                        dto.storyline(),
                        firstRelease,
                        dto.hypes(),
                        coverId,
                        ratings.kr(),
                        ratings.esrb(),
                        modes,
                        perspectives,
                        igdbUpdatedAt
                );

                gamesToUpdate.add(game);
            }
        }

        return gameRepository.saveAll(gamesToUpdate);
    }

    /**
     * 게임 상세 데이터 저장 & 업데이트
     * Tag, Screenshot, Language Support, Company
     * */
    private void syncDetails(List<Game> games, List<IGDBGameDetailResponse> details) {

        //igdbId를 추출해서 detail 응답과 매핑
        Map<Long, IGDBGameDetailResponse> detailMap = details.stream()
                .collect(Collectors.toMap(IGDBGameDetailResponse::igdbId, Function.identity()));

        // 배치 저장을 위한 리스트
        List<Tag> tags = new ArrayList<>();
        List<Screenshot> screenshots = new ArrayList<>();
        List<LanguageSupport> languages = new ArrayList<>();
        List<GameCompany> gameCompanies = new ArrayList<>();

        // 이번 배치에 등장하는 모든 회사 이름 수집 (중복 제거)
        Set<String> companyNames = new HashSet<>();
        for (IGDBGameDetailResponse dto : details) {
            if (dto.companies() != null) {
                dto.companies().stream()
                        .map(IGDBGameDetailResponse.Company::companyDetail)
                        .filter(Objects::nonNull)
                        .map(IGDBGameDetailResponse.Company.CompanyDetail::name) // 이름 추출
                        .filter(name -> name != null && !name.isBlank())
                        .forEach(companyNames::add);
            }
        }

        // DB 조회 & 맵핑 준비 (Key: 회사이름, Value: Company 엔티티)
        Map<String, Company> savedCompanyMap = new HashMap<>();

        if (!companyNames.isEmpty()) {
            // DB에 이미 있는 회사들 조회
            List<Company> existingCompanies = companyRepository.findByNameIn(List.copyOf(companyNames));
            existingCompanies.forEach(c -> savedCompanyMap.put(c.getName(), c));

            // DB에 없는 회사 찾아서 생성
            List<Company> newCompanies = new ArrayList<>();
            for (String name : companyNames) {
                if (!savedCompanyMap.containsKey(name)) {
                    newCompanies.add(Company.builder()
                            .name(name)
                            .build());
                }
            }

            // 신규 회사 저장 후 맵에 추가
            if (!newCompanies.isEmpty()) {
                List<Company> saved = companyRepository.saveAll(newCompanies);
                saved.forEach(c -> savedCompanyMap.put(c.getName(), c));
            }
        }

        // 게임별 상세 데이터 매핑
        for (Game game : games) {
            IGDBGameDetailResponse dto = detailMap.get(game.getIgdbId());
            if (dto == null) continue;

            // Tags (1:1)
            // 장르, 테마, 키워드 이름 정보만 추출해서 저장
            tags.add(Tag.builder()
                    .game(game)
                    .genres(extractValues(dto.genres(), IGDBGameDetailResponse.Genre::name))
                    .themes(extractValues(dto.themes(), IGDBGameDetailResponse.Theme::name))
                    .keywords(extractValues(dto.keywords(), IGDBGameDetailResponse.Keyword::name))
                    .build());

            // Screenshot (1:N)
            if (dto.screenshots() != null) {
                dto.screenshots().forEach(s -> screenshots.add(
                        Screenshot.builder()
                                .game(game).imageId(s.imageId()).width(s.width()).height(s.height())
                                .build()));
            }

            // Language (1:N)
            if (dto.languageSupports() != null) {
                // [언어, 지원 타입] 으로 매핑
                Map<String, Set<String>> langMap = new HashMap<>();

                for (var l : dto.languageSupports()) {
                    // 언어 이름이 없으면 패스
                    if (l.languageInfo() == null || l.languageInfo().name() == null) continue;

                    String langName = l.languageInfo().name();
                    String supportType = (l.supportType() != null) ? l.supportType().name() : "";

                    // 맵에 해당 언어 키가 없으면 생성하고, 지원 타입 추가
                    langMap.computeIfAbsent(langName, k -> new HashSet<>()).add(supportType);
                }

                // (2) 모아진 데이터를 엔티티로 변환 (언어당 1개의 Row 생성)
                for (Map.Entry<String, Set<String>> entry : langMap.entrySet()) {
                    String langName = entry.getKey();
                    Set<String> types = entry.getValue();

                    languages.add(LanguageSupport.builder()
                            .game(game)
                            .language(langName)
                            .voiceSupport(types.contains("Audio"))
                            .subtitle(types.contains("Subtitles"))
                            .interSupport(types.contains("Interface"))
                            .build());
                }
            }

            // GameCompany 매핑 테이블 생성
            if (dto.companies() != null) {
                dto.companies().forEach(c -> {
                    if (c.companyDetail() != null) {
                        String companyName = c.companyDetail().name();
                        Company company = savedCompanyMap.get(companyName);

                        if (company != null) {
                            gameCompanies.add(GameCompany.builder()
                                    .game(game)
                                    .company(company)
                                    .isDeveloper(c.isDeveloper())
                                    .isPublisher(c.isPublisher())
                                    .build());
                        }
                    }
                });
            }


        // 데이터 저장

        // 기존 연관 데이터 삭제
        if (!games.isEmpty()) {
            screenshotRepository.deleteByGameIn(games);
            languageSupportRepository.deleteByGameIn(games);
            gameCompanyRepository.deleteByGameIn(games);
        }

        // Tag는 1:1이므로 바로 saveAll
        tagRepository.saveAll(tags);

        // 나머지 데이터 저장
        screenshotRepository.saveAll(screenshots);
        languageSupportRepository.saveAll(languages);
        gameCompanyRepository.saveAll(gameCompanies);
        }
    }

    // ---------------------------------------------------------------------------------
    // 헬퍼 메서드
    // ---------------------------------------------------------------------------------

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**리스트에서 특정 속성을 추출하는 메서드*/
    private <T> List<String> extractValues(List<T> list, Function<T, String> mapper) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(mapper).toList();
    }

    /**
     * GameSummaryResponse에서 스팀 ID 추출
     * 1. storeAppID 필터링 (steam = 1)
     * 2. String 변환
     * */
    private String extractSteamId(IGDBGameSummaryResponse dto) {
        if (dto.externalApps() == null) return null;
        return dto.externalApps().stream()
                .filter(ext -> ext.storeId() == 1) // Steam Category
                .findFirst()
                .map(ext -> String.valueOf(ext.storeAppid()))
                .orElse(null);
    }

    /**
     * 한글화 타이틀 데이터 추출
     * KR : 2
     * */
    private String extractLocalization(IGDBGameSummaryResponse dto) {
        if (dto.titleLocalization() == null || dto.titleLocalization().isEmpty()) return null;
        return dto.titleLocalization().stream()
                .filter(loc -> loc.region() != null && loc.region().id() == 2L) // Korea = 2
                .map(IGDBGameSummaryResponse.TitleLocalization::name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * 게임 심의 정보 추출, 매핑용
     * */
    private record Ratings(String esrb, String kr) {}

    /**
     * 국내(GRAC), 글로벌(ESRB) 심의 정보 추출
     * */
    private Ratings extractAgeRatings(List<IGDBGameSummaryResponse.AgeRating> ageRatings) {
        if (ageRatings == null || ageRatings.isEmpty()) return new Ratings(null, null);

        String esrb = null;
        String kr = null;

        for (IGDBGameSummaryResponse.AgeRating rating : ageRatings) {
            if (rating.organization() == null || rating.ratingCategories() == null) {
                continue;
            }

            long orgId = rating.organization().id();
            String ratingValue = rating.ratingCategories().rating();

            // 3. 기관 ID에 따라 값 할당
            if (orgId == 1L) {          // ESRB (Global)
                esrb = ratingValue;
            } else if (orgId == 5L) {   // GRAC (Korea)
                kr = ratingValue;
            }

            // 두 값을 모두 찾았으면 루프 종료 (최적화)
            if (esrb != null && kr != null) {
                break;
            }
        }

        return new Ratings(esrb, kr);
    }
}