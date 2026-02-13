package io.github.seoleeder.owls_pick.repository.Custom;

import io.github.seoleeder.owls_pick.entity.game.Dashboard;
import io.github.seoleeder.owls_pick.entity.game.Dashboard.CurationType;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardRepositoryCustom {

    List<Dashboard> findLatestTop100(CurationType type);

    List<Dashboard> findByCurationTypeAndReferenceAt(CurationType type, LocalDateTime referenceAt);
}
