package io.github.seoleeder.owls_pick.client.oauth.provider;


import io.github.seoleeder.owls_pick.global.config.properties.SocialProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

public abstract class AbstractSocialProvider implements SocialAuthProvider {

    protected final RestClient restClient;
    protected final SocialProperties.Registration registration;
    protected final SocialProperties.Provider providerConfig;
    private final String providerName;

    // properties에서 provider 이름에 맞는 설정만 추출
    protected AbstractSocialProvider(String providerName, SocialProperties socialProperties) {
        this.providerName = providerName;
        this.registration = socialProperties.registration().get(providerName);
        this.providerConfig = socialProperties.provider().get(providerName);
        this.restClient = RestClient.builder().build();
    }

    //해당 provider가 지원되는지 확인
    @Override
    public boolean isSupported(String provider) {
        return this.providerName.equalsIgnoreCase(provider);
    }

    // 인가 코드 URL 생성 (모든 소셜 공통 적용)
    @Override
    public String getAuthCodeUrl() {
        //매번 고유의 랜덤 문자열을 생성하여 state 값으로 활용
        String state = UUID.randomUUID().toString();

        return UriComponentsBuilder.fromHttpUrl(providerConfig.authorizationUri())
                .queryParam("client_id", registration.clientId())
                .queryParam("redirect_uri", registration.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", registration.scope()))
                .queryParam("state", state)
                .build()
                .encode() // 인코딩 필수
                .toUriString();
    }
}
