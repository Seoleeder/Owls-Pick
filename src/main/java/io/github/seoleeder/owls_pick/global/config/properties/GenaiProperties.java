package io.github.seoleeder.owls_pick.global.config.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-api.genai")
public record GenaiProperties(
        String fastapiUrl,
        Localization localization,
        Review review
) {
    // 한글화 작업 관련 설정
    public record Localization(
            ChunkSize chunkSize
    ) {
        public record ChunkSize(
                int game,
                int keyword
        ) {}
    }

    // 리뷰 요약 작업 관련 설정
    public record Review(
            int minThreshold,
            int batchSize,
            int maxConcurrentTasks
    ) {}
}