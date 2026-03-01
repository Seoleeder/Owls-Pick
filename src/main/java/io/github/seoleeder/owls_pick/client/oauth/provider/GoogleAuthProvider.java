package io.github.seoleeder.owls_pick.client.oauth.provider;

import io.github.seoleeder.owls_pick.dto.auth.SocialTokenDto;
import io.github.seoleeder.owls_pick.dto.auth.SocialUserResponse;
import io.github.seoleeder.owls_pick.global.config.properties.SocialProperties;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Slf4j
@Component
public class GoogleAuthProvider extends  AbstractSocialProvider{
    private final OidcValidator oidcValidator;

    protected GoogleAuthProvider(SocialProperties socialProperties, OidcValidator oidcValidator) {
        super("google", socialProperties);
        this.oidcValidator = oidcValidator;
    }

    @Override
    public SocialTokenDto fetchAccessToken(String code, String state) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", registration.authorizationGrantType());
        body.add("client_id", registration.clientId());
        body.add("client_secret", registration.clientSecret());
        body.add("redirect_uri", registration.redirectUri());
        body.add("code", code);

        try {
            return restClient.post()
                    .uri(providerConfig.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(SocialTokenDto.class);
        } catch (RestClientResponseException e) {
            log.error("[Google OAuth] 토큰 발급 실패 - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                // 400번대 에러: 프론트가 보낸 Code가 만료되었거나 잘못된 경우
                throw new CustomException(ErrorCode.INVALID_AUTHORIZATION_CODE);
            } else {
                // 500번대 에러: 구글 서버 장애
                throw new CustomException(ErrorCode.OAUTH_SERVER_ERROR);
            }

        } catch (Exception e) {
            log.error("[Google OAuth] 서버 통신 중 알 수 없는 에러 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // ID 토큰 유효성 검증 후, 사용자 정보 추출 (provider id, name, email)
    @Override
    public SocialUserResponse getUserInfo(SocialTokenDto tokenDto) {
        Map<String, Object> claims = oidcValidator.validateAndGetPayload(
                tokenDto.idToken(),
                providerConfig.jwkSetUri()
        );

        // 이메일 데이터가 없는 경우 처리
        String email = (String) claims.get("email");
        if (email == null) {
            log.info("[Google OAuth] 유저가 이메일 제공을 거부했거나 값이 없습니다. (sub: {})", claims.get("sub"));
        }

        return new SocialUserResponse(
                String.valueOf(claims.get(providerConfig.userNameAttribute())), // "sub"
                (String) claims.get("email"),
                (String) claims.get("name")
        );
    }
}
