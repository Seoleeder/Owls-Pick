package io.github.seoleeder.owls_pick.global.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix="cors")
public record CorsProperties (
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,    // 브라우저가 프론트엔드에게 노출할 응답 헤더 목록
        List<String> exposedHeaders,
        Boolean allowCredentials,
        Long maxAge                     // Preflight 캐싱 시간
){}
