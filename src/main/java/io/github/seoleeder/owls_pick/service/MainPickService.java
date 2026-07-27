package io.github.seoleeder.owls_pick.service;

import com.querydsl.core.Tuple;
import io.github.seoleeder.owls_pick.dto.response.UpcomingGameResponse;
import io.github.seoleeder.owls_pick.dto.response.section.PersonalizedSectionResponse;
import io.github.seoleeder.owls_pick.dto.response.section.UpcomingSectionResponse;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.user.User;
import io.github.seoleeder.owls_pick.entity.game.enums.GameSortType;
import io.github.seoleeder.owls_pick.entity.game.enums.GenreType;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import io.github.seoleeder.owls_pick.global.config.properties.CurationProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import io.github.seoleeder.owls_pick.global.util.GameResponseConverter;
import io.github.seoleeder.owls_pick.repository.GameRepository;
import io.github.seoleeder.owls_pick.repository.UserRepository;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import io.github.seoleeder.owls_pick.repository.dto.TagArrayDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainPickService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final CurationProperties curationProps;
    private final GameResponseConverter responseConverter;

    // 유효 장르-테마 교집합 목록 (인메모리 캐싱)
    private volatile List<GenreThemePair> validCombinations = new ArrayList<>();

    public record GenreThemePair(GenreType genre, ThemeType theme) {}

    /**
     * 서버 구동 시 유효 태그 조합 캐시 최초 1회 초기화
     */
    @PostConstruct
    public void initValidCombinations() {
        refreshValidCombinations();
    }

    /**
     * 장르-테마 교집합 캐시 갱신 (최소 게임 수 충족 조합만 필터링)
     * - 주기 : 매일 새벽 4시
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void refreshValidCombinations() {
        log.info("[MainPick] Updating valid genre-theme combinations in memory cache...");

        // 유효 조합 기준 최소 게임 수 조회
        int minRequired = curationProps.intersection().minRequiredGames();

        // 출시된 게임의 전체 태그 배열 조회
        List<TagArrayDto> tagDtos = gameRepository.findTagArraysForReleasedGames();
        Map<GenreThemePair, Long> frequencyMap = new HashMap<>();

        // 태그 배열을 순회하며 장르-테마 교집합 카운트
        for (TagArrayDto dto : tagDtos) {
            List<String> genres = dto.genres();
            List<String> themes = dto.themes();

            if (genres == null || themes == null) continue;

            for (String genreStr : genres) {
                for (String themeStr : themes) {
                    try {
                        GenreType genre = GenreType.valueOf(genreStr);
                        ThemeType theme = ThemeType.valueOf(themeStr);

                        if (theme == ThemeType.EROTIC) continue; // 성인 테마는 교집합에서 배제

                        GenreThemePair pair = new GenreThemePair(genre, theme);
                        frequencyMap.put(pair, frequencyMap.getOrDefault(pair, 0L) + 1L);
                    } catch (IllegalArgumentException e) {
                        log.warn("[MainPick] Ignore unmapped tag data: Genre={}, Theme={}", genreStr, themeStr);
                    }
                }
            }
        }

        // 최소 게임 수를 충족하는 조합 필터링
        List<GenreThemePair> newCombinations = new ArrayList<>();
        for (Map.Entry<GenreThemePair, Long> entry : frequencyMap.entrySet()) {
            if (entry.getValue() >= minRequired) {
                newCombinations.add(entry.getKey());
            }
        }

        // 인메모리 캐시 일괄 갱신
        this.validCombinations = newCombinations;
        log.info("[MainPick] Memory cache updated successfully. Total valid combinations loaded: {}", validCombinations.size());
    }

    /**
     * Upcoming Games: 출시 예정일이 N개월 이내인 출시 예정 기대작 조회
     * */
    @Transactional(readOnly = true)
    public UpcomingSectionResponse getUpcomingGames(Pageable pageable) {
        log.debug("[MainPick] Fetching upcoming games.");

        CurationProperties.Upcoming props = curationProps.upcoming();

        // 조회 기준일 (오늘 ~ N개월 후)
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(props.periodMonths());

        Page<Game> upcomingGames = gameRepository.findUpcomingGames(today, maxDate, props.minHypes(), pageable);

        Page<UpcomingGameResponse> dtoPage = upcomingGames.map(responseConverter::convertToUpcomingDto);

        return new UpcomingSectionResponse("출시 예정 최고 기대작", dtoPage);
    }

    /**
     * [Section 1] Most Personalized Picks: 선호 태그 기반 맞춤 추천
     * 유저 선호 태그 최다 일치 게임 리스트 조회
     */
    public PersonalizedSectionResponse getMostPersonalizedPicks(Long userId, Pageable pageable) {
        log.debug("[MainPick] Fetching most personalized picks for userId: {}", userId);

        // 유저 ID로 사용자 조회
        User user = getUser(userId);

        // 사용자의 선호 태그 목록
        List<String> preferredTags = user.getPreferredTags() != null
                ? new ArrayList<>(user.getPreferredTags())
                : new ArrayList<>();

        // 선호 태그 미설정 시 DB 쿼리 없이 즉시 빈 페이지 반환
        if (preferredTags.isEmpty()) {
            return new PersonalizedSectionResponse("맞춤 픽", responseConverter.convertPage(Page.empty(pageable)));
        }

        // 캐시 적중률 향상을 위한 태그 리스트 오름차순 정렬
        Collections.sort(preferredTags);

        Page<GameWithReviewStatDto> games = gameRepository.findPersonalizedGamesByPreferredTags(preferredTags, pageable);

        return new PersonalizedSectionResponse("맞춤 픽", responseConverter.convertPage(games));
    }

    /**
     * [Section 2] Random Genre Picks: 단일 장르 랜덤 탐색
     */
    public PersonalizedSectionResponse getRandomGenrePicks(Pageable pageable) {
        GenreType[] genres = GenreType.values();

        // 무작위 장르 태그 1개 추출
        GenreType selectedGenre = genres[ThreadLocalRandom.current().nextInt(genres.length)];

        log.debug("[MainPick] Fetching random genre picks. Selected genre: {}", selectedGenre.name());

        Page<GameWithReviewStatDto> games = gameRepository.findGamesByGenre(selectedGenre, GameSortType.POPULAR, pageable);
        return new PersonalizedSectionResponse(selectedGenre.getKorName(), responseConverter.convertPage(games));
    }

    /**
     * [Section 3] Random Theme Picks: 단일 테마 랜덤 탐색 (성인 태그 EROTIC 필터링)
     */
    public PersonalizedSectionResponse getRandomThemePicks(Long userId, Pageable pageable) {

        // 성인 인증 여부 확인
        final boolean isAdult = (userId != null) && getUser(userId).isAdultUser();

        // 미성년자 대상 EROTIC 테마 배제
        List<ThemeType> safeThemes = Arrays.stream(ThemeType.values())
                .filter(theme -> isAdult || theme != ThemeType.EROTIC)
                .toList();

        // 가용 테마 태그 중 1개 무작위 추출
        ThemeType selectedTheme = safeThemes.get(ThreadLocalRandom.current().nextInt(safeThemes.size()));

        log.debug("[MainPick] Fetching random theme picks for userId: {}. Selected theme: {}", userId, selectedTheme.name());

        Page<GameWithReviewStatDto> games = gameRepository.findGamesByTheme(selectedTheme, GameSortType.POPULAR, pageable);
        return new PersonalizedSectionResponse(selectedTheme.getKorName(), responseConverter.convertPage(games));
    }

    /**
     * [Section 4] Intersection Picks: 유효한 장르 X 테마 조합을 가진 게임 조회
     */
    public PersonalizedSectionResponse getIntersectionPicks(Pageable pageable) {
        // 장르 X 테마 캐시 미존재 시 INDIE 태그로 Fallback 처리
        if (validCombinations.isEmpty()) {
            log.warn("[MainPick] No valid combinations found in memory cache. Falling back to INDIE genre.");

            Page<GameWithReviewStatDto> fallbackGames = gameRepository.findGamesByGenre(GenreType.INDIE, GameSortType.POPULAR, pageable);

            return new PersonalizedSectionResponse(GenreType.INDIE.getKorName(), responseConverter.convertPage(fallbackGames));
        }

        // 유효 조합 중 1개 무작위 추출
        GenreThemePair selectedPair = validCombinations.get(ThreadLocalRandom.current().nextInt(validCombinations.size()));

        log.debug("[MainPick] Fetching random intersection picks. Selected combination: [Genre: {}, Theme: {}]",
                selectedPair.genre().name(), selectedPair.theme().name());

        Page<GameWithReviewStatDto> games = gameRepository.findGamesByGenreAndThemeIntersection(
                selectedPair.genre(), selectedPair.theme(), pageable
        );

        // 응답용 조합 타이틀 문자열 조합
        String comboTitle = selectedPair.theme().getKorName() + " " + selectedPair.genre().getKorName();
        return new PersonalizedSectionResponse(comboTitle, responseConverter.convertPage(games));
    }

    /**
     * [Section 5] Hidden Masterpieces: 스코어는 높은데 리뷰 수는 상대적으로 적은 '숨겨진 명작' 게임 조회
     */
    public PersonalizedSectionResponse getHiddenMasterpieces(Pageable pageable) {
        log.debug("[MainPick] Fetching hidden masterpieces.");
        CurationProperties.HiddenMasterpiece props = curationProps.hiddenMasterpiece();
        Page<GameWithReviewStatDto> games = gameRepository.findHiddenMasterpieces(
                props.minReviewScore(),
                props.minReviews(),
                props.maxReviews(),
                pageable
        );

        return new PersonalizedSectionResponse("숨겨진 명작", responseConverter.convertPage(games));
    }

    /**
     * [Section 6] Trending Picks: 요즘 뜨는 특정 태그 게임 조회 (유저 선호 태그 중 택 1, 주간 리뷰 수가 많은 게임 순)
     */
    public PersonalizedSectionResponse getTrendingPicks(Long userId, Pageable pageable) {
        User user = getUser(userId);

        // 선호 태그 미설정 시 즉시 빈 페이지 반환 (Early Return)
        if (user.getPreferredTags() == null || user.getPreferredTags().isEmpty()) {
            return new PersonalizedSectionResponse("트렌딩 픽", responseConverter.convertPage(Page.empty(pageable)));
        }

        // 유저 선호 태그 중 1건 무작위 추출 (연령 필터링 적용)
        String safeTag = getSafeRandomPreferredTag(user);

        log.debug("[MainPick] Fetching trending picks for userId: {}. Selected tag: {}", userId, safeTag);

        Page<GameWithReviewStatDto> games = gameRepository.findTrendingGamesByTag(
                safeTag, curationProps.trending().minReviewScore(), pageable
        );
        return new PersonalizedSectionResponse(safeTag, responseConverter.convertPage(games));
    }

    /**
     * [Section 7] Quick Plays: 플레이타임이 짧고 강렬한 게임 (유저 선호 태그 중 택 1. 리뷰 스코어가 높은 순)
     */
    public PersonalizedSectionResponse getQuickPlays(Long userId, Pageable pageable) {
        User user = getUser(userId);

        // 선호 태그 미설정 시 즉시 빈 페이지 반환 (Early Return)
        if (user.getPreferredTags() == null || user.getPreferredTags().isEmpty()) {
            return new PersonalizedSectionResponse("퀵 플레이", responseConverter.convertPage(Page.empty(pageable)));
        }

        // 유저 선호 태그 중 1건 무작위 추출 (연령 필터링 적용)
        String safeTag = getSafeRandomPreferredTag(user);

        log.debug("[MainPick] Fetching quick plays for userId: {}. Selected tag: {}", userId, safeTag);

        Page<GameWithReviewStatDto> games = gameRepository.findShortPlaytimeGamesByTag(
                safeTag, curationProps.shortPlaytime().maxPlaytime(), curationProps.shortPlaytime().minScore(), pageable
        );
        return new PersonalizedSectionResponse(safeTag, responseConverter.convertPage(games));
    }

    /**
     * 유저 식별자 기반 엔티티 단건 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    /**
     * 유저 선호 태그 무작위 1건 추출 (미성년자 EROTIC 테마 배제)
     */
    private String getSafeRandomPreferredTag(User user) {
        List<String> tags = user.getPreferredTags();

        // 태그 미존재 시 INDIE 폴백
        if (tags == null || tags.isEmpty()) return "INDIE";

        // 미성년자 유해 태그 필터링
        List<String> safeTags = tags;
        if (!user.isAdultUser()) {
            safeTags = tags.stream()
                    .filter(tag -> !tag.equalsIgnoreCase(ThemeType.EROTIC.name()))
                    .toList();
        }

        // 필터링 후 태그 미존재 시 INDIE 폴백
        if (safeTags.isEmpty()) return "INDIE";

        // 정제된 태그 중 무작위 1건 반환
        return safeTags.get(ThreadLocalRandom.current().nextInt(safeTags.size()));
    }
}
