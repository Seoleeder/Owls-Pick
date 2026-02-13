package io.github.seoleeder.owls_pick.client.steamweb;

import io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard.*;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewDetailResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewDetailResponse.SteamReviewDetail;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewStatsResponse;
import io.github.seoleeder.owls_pick.client.steamweb.dto.Review.SteamReviewStatsResponse.SteamReviewStats;
import io.github.seoleeder.owls_pick.client.steamweb.dto.SteamAppListResponse;
import io.github.seoleeder.owls_pick.common.util.TimestampUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamDataCollector {
    private final SteamClient steamClient;

    // 최소 리뷰 수
    private static final int MIN_TOTAL_REVIEWS = 10;

    private static final LocalDateTime MIN_COLLECTION_DATE = LocalDateTime.of(2022, 1, 1, 0, 0);

    /**
     * 스팀 등록 게임 10k 메타데이터 수집 (ID, Title)
     */
    public SteamAppListResponse collectAppList(Long lastAppId) {
        return steamClient.getAppList(lastAppId, 10000);
    }

    /**
     * 전략적 리뷰 수집
     * 리뷰 통계 조회 -> p/n 별로 목표 설정 -> 배치 수집
     * @param minVotesUp votes_up 필터링 (리뷰 유용함 점수)
     */
    public SteamReviewResponse collectRefinedReviews(Long appId, int minVotesUp) {

        // appid에 대한 리뷰 통계 확인
        SteamReviewStatsResponse statsResponse = steamClient.getReviewStat(appId);

        // null 체크
        if (statsResponse == null || statsResponse.querySummary() == null) {
            return null;
        }

        SteamReviewStats summary = statsResponse.querySummary();
        int totalReviews = summary.totalReview();

        // 리뷰가 임계보다 적으면 리소스 낭비 방지를 위해 패스
        if (totalReviews < MIN_TOTAL_REVIEWS) {
            return new SteamReviewResponse(summary, Collections.emptyList());
        }

        // 적응형 샘플링 목표치 계산
        int targetTotal = calculateAdaptiveTarget(totalReviews);

        // 긍정/부정 비율에 따른 할당량 계산
        double posRatio = (double) summary.totalPositive() / totalReviews;
        int posQuota = (int) (targetTotal * posRatio);
        int negQuota = targetTotal - posQuota;

        // 리뷰 상세 데이터 수집
        List<SteamReviewDetail> collectedReviews = new ArrayList<>();
        collectedReviews.addAll(collectReviewByQuota(appId, "positive", posQuota, minVotesUp));
        collectedReviews.addAll(collectReviewByQuota(appId, "negative", negQuota, minVotesUp));

        return new SteamReviewResponse(summary,collectedReviews);
    }

    /**
     * 특정 타입(긍정/부정) 리뷰에 대해 목표치만큼 수집
     */
    public List<SteamReviewDetail> collectReviewByQuota(Long appId, String reviewType, int quota, int minVotesUp) {
        List<SteamReviewDetail> collected = new ArrayList<>();
        String cursor = "*"; //커서 초기화

        while (collected.size() < quota) {
            try{
                // 배치 단위 수집 요청 (num_per_page = 100)
                SteamReviewDetailResponse response = steamClient.getReviewDetail(appId, cursor, reviewType);

                if (response == null || response.reviews() == null || response.reviews().isEmpty()) {
                    break; // 더 이상 데이터가 없으면 중단
                }

                // 유용함 점수가 임계값 이상인 리뷰들만 선별
                List<SteamReviewDetailResponse.SteamReviewDetail> filtered = response.reviews().stream()
                        .filter(r -> r.votesUp() >= minVotesUp)
                        .toList();

                // 목표 개수만큼만 잘라서 추가
                int needed = quota - collected.size();
                if (filtered.size() > needed) {
                    collected.addAll(filtered.subList(0, needed));
                } else {
                    collected.addAll(filtered);
                }

                // 커서 갱신
                cursor = response.cursor();

                // 커서 무한루프 방지
                if (cursor == null) break;
            }catch (Exception e) {

                // 수집 성공한 리뷰들만 모아서 리턴
                log.warn("Failed to fetch review page for AppID {} (Type: {}). Stopping collection. Reason: {}",
                        appId, reviewType, e.getMessage());

                break;
            }
        }

        return collected;
    }

    /**
     * 주간 최고 매출 게임 랭크 데이터 수집
     * @param countryCode 집계가 반영되는 국가 코드 (대한민국 = KR)
     * @param pageStart 해당 페이지에서 몇위부터 반환할지 명시
     * @param pageCount 페이지당 가져올 게임의 개수 (매출별로 정렬)
     */
    public SteamDashboardResponse collectWeeklyTopSellers (String countryCode, Long startDate, Integer pageStart, Integer pageCount){
        SteamWeeklyTopSellersResponse response = steamClient.getWeeklyTopSeller(countryCode, startDate, pageStart, pageCount);

        if (response == null || response.response() == null) {
            return null;
        }

        // steam 상의 timestamp를 LocalDateTime으로 변환
        LocalDateTime collectedDate = TimestampUtils.toLocalDateTime(response.response().startDate());

        if (collectedDate.isBefore(MIN_COLLECTION_DATE)){
            return null;
        }

        //WeeklyTopSellerDTO를 Steam 대시보드 DTO와 매핑
        List<SteamDashboardResponse.Rank> ranks = response.response().ranks()
                .stream().map(rank -> new SteamDashboardResponse.Rank(rank.rank(), rank.appid())).toList();

        return new SteamDashboardResponse(collectedDate, ranks);
    }

    /**
     * 연간 스팀 우수작 데이터 수집
     * @param rtimeYear 기준이 되는 timestamp (연도)
     * */
    public SteamDashboardResponse collectYearTopApp(Long rtimeYear){
        SteamYearOrMonthTopAppResponse response = steamClient.getYearTopApp(rtimeYear);
        
        if(response == null || response.response() == null){
            return null;
        }

        LocalDateTime collectedYear = TimestampUtils.toLocalDateTime(rtimeYear);
        if(collectedYear.isBefore(MIN_COLLECTION_DATE)){
            return null;
        }

        List<SteamDashboardResponse.Rank> ranks = response.response().topApp().stream()
                .map(top -> new SteamDashboardResponse.Rank(top.rank(), top.appId()))
                .toList();
        
        return new SteamDashboardResponse(collectedYear, ranks);

    }

    /**
     * 월간 스팀 우수작 데이터 수집
     * @param rtimeMonth 기준이 되는 timestamp (월)
     * */
    public SteamDashboardResponse collectMonthTopApp(Long rtimeMonth){
        SteamYearOrMonthTopAppResponse response = steamClient.getMonthTopApp(rtimeMonth);

        if(response == null || response.response() == null){
            return null;
        }

        LocalDateTime collectedMonth = TimestampUtils.toLocalDateTime(rtimeMonth);
        if(collectedMonth.isBefore(MIN_COLLECTION_DATE)){
            return null;
        }

        List<SteamDashboardResponse.Rank> ranks = response.response().topApp().stream()
                .map(top -> new SteamDashboardResponse.Rank(top.rank(), top.appId()))
                .toList();

        return new SteamDashboardResponse(collectedMonth, ranks);

    }

    /**
     * 스팀 게임 최다 동시 접속자수 랭크 데이터 수집 (15분 간격 업데이트)
     * @param countryCode 랭크가 집계되는 국가 코드 (KR)
     * */
    public SteamDashboardResponse collectConcurrentPlayersTopApp(String countryCode) {
        SteamConcurrentPlayersTopAppResponse response = steamClient.getConcurrentPlayersTopApp(countryCode);

        if (response == null || response.ranks() == null) {
            return null;
        }

        LocalDateTime updateAt = TimestampUtils.toLocalDateTime(response.updatedAt());

        List<SteamDashboardResponse.Rank> ranks = response.ranks().stream()
                .map(r -> new SteamDashboardResponse.Rank(r.rank(), r.appid()))
                .toList();

        return new SteamDashboardResponse(updateAt, ranks);
    }

    /**
     * 스팀 최다 플레이 게임 랭크 데이터 수집 (일일 업데이트)
     * @param countryCode 랭크가 집계되는 국가 코드(KR)
     * */
    public SteamDashboardResponse collectMostPlayedApp(String countryCode) {
        SteamMostPlayedAppResponse response = steamClient.getMostPlayedApp(countryCode);

        if (response == null || response.ranks() == null) {
            return null;
        }

        LocalDateTime updateAt = TimestampUtils.toLocalDateTime(response.updatedAt());

        List<SteamDashboardResponse.Rank> ranks = response.ranks().stream()
                .map(r -> new SteamDashboardResponse.Rank(r.rank(), r.appid()))
                .toList();

        return new SteamDashboardResponse(updateAt, ranks);
    }

    /**
     * 적응형 샘플링 계산 로직
     * - 100개 미만: 전체 수집
     * - 1000개 미만: 200개 샘플링
     * - 1000개 초과: 500개 샘플링
     */
    private int calculateAdaptiveTarget(int total) {
        if (total < 100) return total;
        if (total < 1000) return 200;
        return 500;
    }
}
