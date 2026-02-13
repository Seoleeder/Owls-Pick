package io.github.seoleeder.owls_pick.client.IGDB;

import io.github.seoleeder.owls_pick.client.IGDB.dto.TwitchTokenResponse;
import io.github.seoleeder.owls_pick.common.config.properties.IgdbProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/** IGDB에서 사용할 Access Token 발급을 위한 객체
 * 발급받은 Token을 Redis에 저장
 * 만료 시각에 대해 5분의 마진을 둬서 안정적인 토큰 발급 가능
 * */
@Slf4j
@Component
public class IGDBAuthManager {

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private final IgdbProperties props;

    private static final String KEY_IGDB_ACCESS_TOKEN = "auth:igdb:token";

    public IGDBAuthManager(RestClient restClient, StringRedisTemplate redisTemplate, IgdbProperties props) {
        this.restClient = restClient;
        this.redisTemplate = redisTemplate;
        this.props = props;
    }

    /** Redis에 캐시로 저장된 유효한 ACCESS TOKEN이 있는지 확인하고 없으면 발급
     */
    public String getAccessToken() {

        // Redis 캐시 확인
        String cachedToken = redisTemplate.opsForValue().get(KEY_IGDB_ACCESS_TOKEN);
        if (cachedToken != null) {
            return cachedToken;
        }

        return requestAccessToken();
    }

    /** IGDB에 Access Token 발급 요청
     *  synchronized -> IGDB의 Rate limit로 인해 생길 수 있는 문제 방지
     * */
    public synchronized String requestAccessToken(){
        String cachedToken = redisTemplate.opsForValue().get(KEY_IGDB_ACCESS_TOKEN);
        if (cachedToken != null) return cachedToken;

        log.info("Requesting new IGDB Access Token...");

        TwitchTokenResponse response = restClient
                .post().uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(props.authUrl())
                        .path("/oauth2/token")
                        .queryParam("client_id",props.clientId())
                        .queryParam("client_secret", props.clientSecret())
                        .queryParam("grant_type", "client_credentials")
                        .build())
                .retrieve()
                .body(TwitchTokenResponse.class);

        if(response != null && response.accessToken() != null){

            //토큰 만료 마진 5분 설정
            long ttl = response.expiresIn() - 300;

            redisTemplate.opsForValue().set(
                    KEY_IGDB_ACCESS_TOKEN,
                    response.accessToken(),
                    ttl,
                    TimeUnit.SECONDS
            );

            log.info("Token Refreshed! Expires in: {}s", ttl);

            return response.accessToken();
        }
        throw new RuntimeException("Failed to get IGDB Token");
    }


}
