package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Review;
import io.github.seoleeder.owls_pick.repository.Custom.ReviewRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {
    //특정 게임 내에서 해당 리뷰 ID가 존재하는지 확인
    boolean existsByGameIdAndRecommendationId(Long gameId, Long recommendationId);
}
