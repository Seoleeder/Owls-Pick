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
public class NaverAuthProvider extends AbstractSocialProvider {

    private final OidcValidator oidcValidator;

    public NaverAuthProvider(SocialProperties socialProperties, OidcValidator oidcValidator) {
        super("naver", socialProperties);
        this.oidcValidator = oidcValidator;
    }

    @Override
    public SocialTokenDto fetchAccessToken(String code, String state) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", registration.authorizationGrantType());
        body.add("client_id", registration.clientId());
        body.add("client_secret", registration.clientSecret());
        body.add("code", code);
        body.add("state", state); // 네이버는 state 필수

        try {
            return restClient.post()
                    .uri(providerConfig.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(SocialTokenDto.class);
        } catch (RestClientResponseException e) {
            log.error("[Naver OAuth] 토큰 발급 실패 - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is4xxClientError()) {
                // 인가코드가 만료되었거나 잘못됨
                throw new CustomException(ErrorCode.INVALID_AUTHORIZATION_CODE);
            } else {
                // 500번대 에러: 네이버 서버 장애
                throw new CustomException(ErrorCode.OAUTH_SERVER_ERROR);
            }

        } catch (Exception e) {
            log.error("[Naver OAuth] 서버 통신 중 알 수 없는 에러 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public SocialUserResponse getUserInfo(SocialTokenDto tokenDto) {
        // 네이버의 id token의 유효성 검증 -> payload 추출
        Map<String, Object> claims = oidcValidator.validateAndGetPayload(
                tokenDto.idToken(),
                providerConfig.jwkSetUri()
        );

        // payload에서 provider id 추출
        String verifiedProviderId = (String) claims.get(providerConfig.userNameAttribute()); // "sub"

        // 상세 정보(이메일, 이름)를 담을 DTO
        record NaverResponse(String resultcode, Response response) {
            record Response(String email, String name) {
            }
        }

        NaverResponse naverResponse = restClient.get()
                .uri(providerConfig.userInfoUri())
                .header("Authorization", "Bearer " + tokenDto.accessToken())
                .retrieve()
                .body(NaverResponse.class);

        if (naverResponse == null || !"00".equals(naverResponse.resultcode())) {
            throw new IllegalArgumentException("네이버 유저 정보 조회 실패");
        }

        // 이메일 데이터가 없는 경우 처리
        if (naverResponse.response.email == null) {
            log.info("[Kakao OAuth] 유저가 이메일 제공을 거부했거나 값이 없습니다. (sub: {})", claims.get("sub"));
        }

        // provider id와 사용자 정보 결합
        return new SocialUserResponse(
                verifiedProviderId,
                naverResponse.response().email(),
                naverResponse.response().name()
        );
    }
}