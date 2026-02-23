package io.github.seoleeder.owls_pick.client.oauth.factory;

import io.github.seoleeder.owls_pick.client.oauth.provider.SocialAuthProvider;
import io.github.seoleeder.owls_pick.global.response.CustomException;
import io.github.seoleeder.owls_pick.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SocialAuthFactory {

    // SocialAuthProvider를 상속받은 모든 빈(카카오, 구글, 네이버)을 리스트로 묶음
    private final List<SocialAuthProvider> providers;

    // providerName을 받아 해당하는 Provider 구현체를 반환
    public SocialAuthProvider getProvider(String providerName) {
        return providers.stream()
                .filter(provider -> provider.isSupported(providerName))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_PROVIDER));
    }
}