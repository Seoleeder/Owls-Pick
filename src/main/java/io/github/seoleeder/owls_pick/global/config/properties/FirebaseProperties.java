package io.github.seoleeder.owls_pick.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *  Firebase 설정 매핑
 */
@ConfigurationProperties(prefix = "firebase.config")
public record FirebaseProperties(
        // Firebase Admin SDK 초기화를 위한 서비스 계정 키(JSON 파일)의 경로 매핑
        String path
) {
}
