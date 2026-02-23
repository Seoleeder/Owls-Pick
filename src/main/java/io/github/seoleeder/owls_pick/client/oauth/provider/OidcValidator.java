package io.github.seoleeder.owls_pick.client.oauth.provider;

import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OidcValidator {

    // 소셜 서비스(카카오, 구글)마다 JWK Set URI가 다르므로, 각각의 Decoder를 캐싱
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    /**
     * ID 토큰의 서명(JWK)과 만료일(exp)을 검증하고 페이로드를 반환합니다.
     */
    public Map<String, Object> validateAndGetPayload(String idToken, String jwkSetUri) {

        // 해당 jwkSetUri에 맞는 JwtDecoder를 가져오거나 없으면 새로 생성 (내부적으로 외부 JWK 통신 및 캐싱 수행)
        JwtDecoder decoder = decoders.computeIfAbsent(jwkSetUri, uri -> NimbusJwtDecoder.withJwkSetUri(uri).build());

        try {
            // 서명 검증, 만료일(exp) 검증
            Jwt jwt = decoder.decode(idToken);

            // 완벽하게 안전성이 보장된 Payload(Claims)만 반환
            return jwt.getClaims();

        } catch (JwtException e) {
            // 서명이 다르거나 만료된 토큰
            log.warn("유효하지 않은 OIDC ID Token 입니다: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            // ✨ 2차 변환: 디코더 생성 중 외부 JWK 통신이 터지는 등의 예상치 못한 에러
            log.error("OIDC 검증 중 서버 에러 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
