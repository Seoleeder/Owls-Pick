package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.entity.game.VectorEmbedding;
import io.github.seoleeder.owls_pick.repository.Custom.VectorEmbeddingRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QVectorEmbedding.vectorEmbedding;

@Repository
@RequiredArgsConstructor
public class VectorEmbeddingRepositoryImpl implements VectorEmbeddingRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * Game ID 목록에 해당하는 기존 임베딩 데이터 일괄 조회
     */
    @Override
    public List<VectorEmbedding> findExistingEmbeddingsByGameIds(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(vectorEmbedding)
                .where(vectorEmbedding.game.id.in(gameIds))
                .fetch();
    }
}
