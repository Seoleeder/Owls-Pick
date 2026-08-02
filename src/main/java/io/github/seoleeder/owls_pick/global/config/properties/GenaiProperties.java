package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-api.genai")
public record GenaiProperties(
        String fastapiUrl,
        String callbackBaseUrl,
        FailedTask failedTask,
        Localization localization,
        Review review,
        Embedding embedding,
        Chat chat
) {
    // 실패 작업(Failed Task) 관리 관련 설정
    public record FailedTask(
            int retentionDays
    ) {}

    // 한글화 작업 관련 설정
    public record Localization(
            int delayMs,
            Game game,
            Keyword keyword
    ) {
        // 게임 데이터 한글화 전용 설정
        public record Game(
                int chunkSize
        ) {}

        // 키워드 한글화 전용 설정
        public record Keyword(
                int chunkSize,
                int dbBatchSize
        ) {}
    }

    // 리뷰 요약 작업 관련 설정
    public record Review(
            int minThreshold,
            int batchSize,
            int delayMs
    ) {}

    // 벡터 임베딩 작업 관련 설정
    public record Embedding(
            int dbFetchSize,
            int apiBatchSize,
            int delayMs
    ) {}

    // RAG 기반 Owls 챗봇 관련 설정
    public record Chat(
            int historyLimit,
            Traffic traffic
    ){
        // 챗봇 트래픽 제어 인프라 설정
        public record Traffic(
                Prefix prefix,
                Limit limit
        ) {
            public record Prefix(
                    String lock,
                    String rateLimit
            ) {}

            public record Limit(
                    int maxRequestsPerMinute
            ) {}
        }
    }
}