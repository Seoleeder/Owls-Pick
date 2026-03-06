package io.github.seoleeder.owls_pick.repository.Custom;

import java.time.LocalDateTime;

public interface ReviewStatRepositoryCustom {
    //특정 게임의 주간 리뷰 수 업데이트
    void updateWeeklyReviewCount(Long gameId, LocalDateTime startTime);
}
