package io.github.seoleeder.owls_pick.common.config.redis; // 패키지명은 프로젝트에 맞게!

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTestRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== Redis Connection Test Start ==========");

        try {
            // 1. 데이터 저장 (Key: "test:hello", Value: "world")
            redisTemplate.opsForValue().set("test:hello", "world");
            log.info("1. Data Saved: test:hello -> world");

            // 2. 데이터 조회
            String value = redisTemplate.opsForValue().get("test:hello");
            log.info("2. Data Retrieved: {}", value);

            if ("world".equals(value)) {
                log.info("Redis Connection SUCCESS!");
            } else {
                log.error("Redis Value Mismatch");
            }
        } catch (Exception e) {
            log.error("Redis Connection FAILED: {}", e.getMessage());
        }

        log.info("===============================================");
    }
}
