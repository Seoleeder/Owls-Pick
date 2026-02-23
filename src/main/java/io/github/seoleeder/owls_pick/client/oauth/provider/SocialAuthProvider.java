package io.github.seoleeder.owls_pick.client.oauth.provider;

import io.github.seoleeder.owls_pick.dto.auth.SocialTokenDto;
import io.github.seoleeder.owls_pick.dto.auth.SocialUserResponse;

public interface SocialAuthProvider {
    boolean isSupported(String provider);
    String getAuthCodeUrl();
    SocialTokenDto fetchAccessToken(String code, String state);
    SocialUserResponse getUserInfo(SocialTokenDto tokenDto);
}
