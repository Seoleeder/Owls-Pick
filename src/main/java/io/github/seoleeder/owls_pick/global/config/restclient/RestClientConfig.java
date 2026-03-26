package io.github.seoleeder.owls_pick.global.config.restclient;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;


@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final RestClientLoggingInterceptor loggingInterceptor;

    @Bean
    public RestClient restClient() {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)) // 연결 시도 3초 제한
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(10));   // 응답 대기 10초 제한

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(loggingInterceptor)
                .build();
    }

    /**
     * GEMINI 한글화 엔진(FastAPI) 전용 RestClient 빈
     */
    @Bean("localizationRestClient")
    public RestClient localizationRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);

        // 네트워크 지연 및 AI 생성 시간 변동폭을 고려하여 응답 대기 30초 제한
        factory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(loggingInterceptor)
                .build();
    }
}
