package io.github.seoleeder.owls_pick.repository.Custom;

import io.github.seoleeder.owls_pick.entity.game.VectorEmbedding;

import java.util.List;

public interface VectorEmbeddingRepositoryCustom {

    // Game ID 목록에 해당하는 기존 임베딩 데이터 일괄 조회
    List<VectorEmbedding> findExistingEmbeddingsByGameIds(List<Long> gameIds);
}
