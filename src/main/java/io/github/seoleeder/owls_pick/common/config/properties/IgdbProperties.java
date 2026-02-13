package io.github.seoleeder.owls_pick.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api.igdb")
public record IgdbProperties(
        String clientId,
        String clientSecret,
        String baseUrl,
        String authUrl
) {}
