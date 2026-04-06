package io.github.seoleeder.owls_pick.client.steam;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.seoleeder.owls_pick.client.steam.dto.Review.SteamReviewDetailResponse;
import io.github.seoleeder.owls_pick.client.steam.dto.Review.SteamReviewStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스팀 API 통신 전담 Caller (Resilience4j 방어막 계층)
 */
@Component
@RequiredArgsConstructor
public class SteamApiCaller {
    private final SteamClient steamClient;

    private static final String STEAM_API = "steamApi";

    /**
     * @RateLimiter: yml에 설정한 초당 허용량만큼만 통과
     * @Retry: 타임아웃이나 429 에러 발생 시 지정된 횟수만큼 자동 재시도
     */
    @RateLimiter(name = STEAM_API)
    @Retry(name = STEAM_API)
    public SteamReviewStatsResponse getReviewStatSafe(Long appId) {
        return steamClient.getReviewStat(appId);
    }

    @RateLimiter(name = STEAM_API)
    @Retry(name = STEAM_API)
    public SteamReviewDetailResponse getReviewDetailSafe(Long appId, String cursor, String reviewType) {

        return steamClient.getReviewDetail(appId, cursor, reviewType);
    }
}
