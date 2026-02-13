package io.github.seoleeder.owls_pick.common.config.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // [Request 로그] 어떤 메서드로 어디를 찔렀는지 기록
        log.debug(">>> [API Request] {} {}", request.getMethod(), request.getURI());

        ClientHttpResponse response = execution.execute(request, body);

        // [Response 로그] 결과가 성공(200)인지 실패(404, 405 등)인지 기록
        log.debug("<<< [API Response] Status: {}", response.getStatusCode());

        return response;
    }
}
