package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.repository.Custom.GameRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

import static io.github.seoleeder.owls_pick.entity.game.QGame.game;

@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<LocalDateTime> findMaxIgdbUpdatedAt() {

            LocalDateTime maxDateTime = queryFactory
                    .select(game.igdbUpdatedAt.max())
                    .from(game)
                    .fetchOne();

            return Optional.ofNullable(maxDateTime);
    }

}
