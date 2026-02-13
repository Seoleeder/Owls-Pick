package io.github.seoleeder.owls_pick.common.config.restclient;

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
        factory.setReadTimeout(Duration.ofSeconds(60));   // 응답 대기 10초 제한

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(loggingInterceptor)
                .build();
    }
}
