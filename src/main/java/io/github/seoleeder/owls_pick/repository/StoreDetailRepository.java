package io.github.seoleeder.owls_pick.repository;

import io.github.seoleeder.owls_pick.entity.game.Game;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail;
import io.github.seoleeder.owls_pick.entity.game.StoreDetail.StoreName;
import io.github.seoleeder.owls_pick.repository.custom.StoreDetailRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreDetailRepository extends JpaRepository<StoreDetail, Long>, StoreDetailRepositoryCustom {

    // 특정 게임의 모든 스토어 상세 정보 조회
    List<StoreDetail> findByGameId(Long gameId);

}
