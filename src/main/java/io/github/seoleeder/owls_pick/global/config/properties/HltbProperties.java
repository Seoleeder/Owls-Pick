package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.hltb")
public record HltbProperties(
        String fastapiUrl,
        int chunkSize
) {
}
