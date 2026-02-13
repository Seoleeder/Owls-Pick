package io.github.seoleeder.owls_pick.repository.Impl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.github.seoleeder.owls_pick.entity.game.Dashboard;
import io.github.seoleeder.owls_pick.entity.game.QDashboard;
import io.github.seoleeder.owls_pick.repository.Custom.DashboardRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.seoleeder.owls_pick.entity.game.QDashboard.dashboard;
import static io.github.seoleeder.owls_pick.entity.game.QGame.game;

@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Dashboard> findLatestTop100(Dashboard.CurationType type) {

        // 서브쿼리용 별칭 생성
        QDashboard subDashboard = new QDashboard("subDashboard");

        return queryFactory.selectFrom(dashboard)
                .join(dashboard.game, game).fetchJoin()
                .where(
                        dashboard.curationType.eq(type),
                        dashboard.referenceAt.eq(
                                JPAExpressions
                                        .select(subDashboard.referenceAt.max())
                                        .from(subDashboard)
                                        .where(subDashboard.curationType.eq(type))
                        )
                )
                .orderBy(dashboard.rank.asc())
                .fetch();
    }

    @Override
    public List<Dashboard> findByCurationTypeAndReferenceAt(Dashboard.CurationType type, LocalDateTime referenceAt) {
        return queryFactory
                .selectFrom(dashboard)
                .join(dashboard.game).fetchJoin()
                .where(
                        dashboard.curationType.eq(type),
                        dashboard.referenceAt.eq(referenceAt)
                )
                .orderBy(dashboard.rank.asc())
                .fetch();
    }
}
