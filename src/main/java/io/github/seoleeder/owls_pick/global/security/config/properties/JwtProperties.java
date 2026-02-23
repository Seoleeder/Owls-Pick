package io.github.seoleeder.owls_pick.global.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties (
        String header,  // jwt를 담을 헤더 이름
        String secret,  // jwt 서명에 사용할 비밀 키
        Long expiration,
        Long refreshTokenValidity   // refresh token 만료 시간
){}
