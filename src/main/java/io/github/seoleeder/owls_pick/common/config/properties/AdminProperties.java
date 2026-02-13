package io.github.seoleeder.owls_pick.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "owls-pick")
public record AdminProperties(
        String adminKey
) {
}
