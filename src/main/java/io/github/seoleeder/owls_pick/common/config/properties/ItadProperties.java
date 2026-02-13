package io.github.seoleeder.owls_pick.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.itad")
public record ItadProperties(
        String key,
        String baseUrl,
        int batchSize
) {
}
