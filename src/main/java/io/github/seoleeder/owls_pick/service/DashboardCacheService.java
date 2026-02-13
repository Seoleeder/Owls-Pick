package io.github.seoleeder.owls_pick.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seoleeder.owls_pick.dto.DashboardCacheDto;
import io.github.seoleeder.owls_pick.entity.game.Dashboard;
import io.github.seoleeder.owls_pick.entity.game.Dashboard.CurationType;
import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.repository.DashboardRepository;
import io.github.seoleeder.owls_pick.repository.StoreDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DashboardRepository dashboardRepository;
    private final StoreDetailRepository storeDetailRepository;
    private final ObjectMapper objectMapper;

    // 가격 변동 반영을 위해 30분마다 캐시 만료 (랭킹은 유지되더라도 가격은 갱신됨)
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * [Cache] 대시보드 데이터 조회
     * 1. Redis에 있으면 즉시 반환
     * 2. 없으면 DB에서 최신 데이터 조합 후 캐싱하고 반환
     */
    public List<DashboardCacheDto> getDashboard(CurationType type) {
        String key = getKey(type);

        // Redis 조회
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            // Redis에서 꺼낸 JSON을 DTO 리스트로 안전하게 변환
            return objectMapper.convertValue(cached, new TypeReference<>() {});
        }

        // 캐시 Miss -> DB에서 새로고침 후 반환
        return refreshCache(type);
    }

    /**
     * [갱신] DB의 최신 랭킹 + 현재 가격 -> Redis 저장
     * 수집기가 호출하거나, 캐시 만료 시 호출됨
     */
    @Transactional(readOnly = true)
    public List<DashboardCacheDto> refreshCache(CurationType type) {
        log.info("Refreshing Dashboard Cache for: {}", type);

        try {

            // 가장 최신의 대시보드 데이터 조회
            List<Dashboard> rankings = dashboardRepository.findLatestTop100(type);

            if (rankings.isEmpty()) {
                return List.of();
            }

            // 대시보드에서 게임 ID만 추출
            List<Long> gameIds = rankings.stream()
                    .map(d -> d.getGame().getId())
                    .toList();

            // 해당 게임들의 Steam 가격 정보 조회
            List<StoreDetail> steamDetails = storeDetailRepository.findDetailsByStoreAndGameIds(
                    StoreName.STEAM,
                    gameIds
            );

            // GameID -> StoreDetail 로 매핑
            Map<Long, StoreDetail> priceMap = steamDetails.stream()
                    .collect(Collectors.toMap(
                            detail -> detail.getGame().getId(), // Key: Game ID
                            Function.identity(),        // Value: StoreDetail 객체
                            (oldVal, newVal) -> oldVal  // 중복 방지
                    ));

            // Dashboard + Price -> DTO 생성
            List<DashboardCacheDto> dtoList = rankings.stream()
                    .map(rank -> {
                        Game game = rank.getGame();
                        StoreDetail price = priceMap.get(game.getId());

                        return DashboardCacheDto.builder()
                                .gameId(game.getId())
                                .title(game.getTitle())
                                .coverId(game.getCoverId())
                                .curationType(type)
                                .rank(rank.getRank())
                                // 가격 정보
                                .originalPrice(price != null ? price.getOriginalPrice() : null)
                                .discountPrice(price != null ? price.getDiscountPrice() : null)
                                .discountRate(price != null ? price.getDiscountRate() : null)
                                // 데이터 기준 시간 (집계 시각)
                                .referenceAt(rank.getReferenceAt())
                                .build();
                    })
                    .toList();

            // Redis 저장 (TTL 30 minutes)
            try {
                redisTemplate.opsForValue().set(getKey(type), dtoList, CACHE_TTL);
                log.info("Redis Cache Updated Successfully");
            } catch (Exception e) {
                // Redis 에러는 로그만 남기고 무시
                log.error("Redis Connection Failed! Cache NOT updated. (Type: {})", type, e);
            }

            return dtoList;
        } catch (Exception e) {
            log.error("Critical Error during Cache Refresh", e);
            return List.of(); // 빈 리스트 반환
        }
    }

    private String getKey(CurationType type) {
        return "dashboard:" + type.name();
    }
}
