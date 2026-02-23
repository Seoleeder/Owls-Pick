package io.github.seoleeder.owls_pick.global.security.config;

import io.github.seoleeder.owls_pick.global.config.properties.SocialProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SocialProperties.class)
public class SocialConfig {

    private final SocialProperties socialProperties;

    /**
     * OAuth2 설정의 Registration과 Provider 간의 정합성 검증
     * */
    @PostConstruct
    public void validate(){
        log.debug("Social Login Configuration loading and validating...");

        Set<String> registrationKeys = socialProperties.registration().keySet();
        Set<String> providerKeys = socialProperties.provider().keySet();

        // Registration에 정의된 소셜 서비스가 Provider 메타데이터를 가지고 있는지 확인
        for (String key : registrationKeys) {
            if (!providerKeys.contains(key)) {
                log.error("Configuration Error: Provider metadata missing for '{}'", key);
                throw new IllegalStateException("Missing provider for: " + key);
            }
        }

        // 2. Log loaded providers for visibility
        log.info("Successfully loaded social providers: {}", registrationKeys);
    }

}


