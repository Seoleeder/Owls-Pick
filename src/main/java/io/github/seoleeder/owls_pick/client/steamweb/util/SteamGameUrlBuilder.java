package io.github.seoleeder.owls_pick.client.steamweb.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SteamGameUrlBuilder {

    @Value("${external-api.steam.base-url.store}")
    private String STEAM_STORE_BASE_URL;

    /**
     * Steam에서 수집한 AppId로 Store URL 생성
     * @param steamAppId url을 받을 스팀 app id
     * */
    public String buildUrl (String steamAppId){
        if(steamAppId == null || steamAppId.isBlank()){
            return null;
        }
        return "https://" + STEAM_STORE_BASE_URL + "/app/" + steamAppId;
    }
}
