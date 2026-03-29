package io.github.seoleeder.owls_pick.client.itad;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.seoleeder.owls_pick.client.itad.dto.ItadBulkResponse;
import io.github.seoleeder.owls_pick.client.itad.dto.ItadPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ITAD API 통신 전담 Caller (Resilience4j 방어막 계층)
 */
@Component
@RequiredArgsConstructor
public class ItadApiCaller {

    private final ItadClient itadClient;

    private static final String ITAD_API = "itadApi";

    /**
     * @RateLimiter: yaml에 설정한 초당 허용량만큼만 통과
     * @Retry: 타임아웃이나 429 에러 발생 시 지정된 횟수만큼 자동 재시도
     */
    @RateLimiter(name = ITAD_API)
    @Retry(name = ITAD_API)
    public String findItadIdSafe(String steamId) {
        return itadClient.findItadIdBySteamId(steamId);
    }

    @RateLimiter(name = ITAD_API)
    @Retry(name = ITAD_API)
    public ItadBulkResponse findItadIdsBulkSafe(int shopId, List<String> formattedSteamIds) {
        return itadClient.findItadIdsBulk(shopId, formattedSteamIds);
    }

    @RateLimiter(name = ITAD_API)
    @Retry(name = ITAD_API)
    public List<ItadPriceResponse> getPricesSafe(List<String> itadIds) {
        return itadClient.getPrices(itadIds);
    }
}