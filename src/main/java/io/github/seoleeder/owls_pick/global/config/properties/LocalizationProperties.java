package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 한글화 엔진 연동 환경 변수 매핑
 */
@ConfigurationProperties(prefix = "external-api.localization")
public record LocalizationProperties(
        String baseUrl,
        ChunkSize chunkSize
) {
    public record ChunkSize(
            int game,
            int keyword
    ) {}
}