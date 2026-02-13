package io.github.seoleeder.owls_pick.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "external-api.steam")
public record SteamProperties(
        String key,
        BaseUrl baseUrl,
        Sync sync,
        Review review,
        Dashboard dashboard
) {
    public record BaseUrl(String store, String web) {}
    public record Sync(
            int threadPoolSize // 스레드 풀 사이즈
    ) {}
    public record Review(
            int minVotesUp,                 // 리뷰 최소 유용함 수
            int initBatchSize,              // 초기 데이터 수집용 배치
            int maintenanceBatchSize        // 업데이트용 배치
    ) {}
    public record Dashboard(
            LocalDate minCollectionDate,    // 수집 시작 기준일 (2022-01-01)
            String countryCode,             // 국가 코드
            int pageCount                   // 페이지 내 데이터 수
    ) {}
}
