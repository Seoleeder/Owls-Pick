package io.github.seoleeder.owls_pick.repository.Custom.game;

import io.github.seoleeder.owls_pick.dto.request.GameSearchConditionRequest;
import io.github.seoleeder.owls_pick.dto.response.SearchFilterMetadataResponse;
import io.github.seoleeder.owls_pick.repository.dto.GameWithReviewStatDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GameSearchRepositoryCustom {
    //통합 검색 쿼리
    Page<GameWithReviewStatDto> searchGames(GameSearchConditionRequest condition, Pageable pageable);

    // 필터 슬라이더를 위한 메타데이터 조회
    SearchFilterMetadataResponse.PriceRange getPriceRange();
    SearchFilterMetadataResponse.PlaytimeRange getPlaytimeRange();
}
