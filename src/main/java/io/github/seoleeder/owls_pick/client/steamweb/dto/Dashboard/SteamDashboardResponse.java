package io.github.seoleeder.owls_pick.client.steamweb.dto.Dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record SteamDashboardResponse(
        LocalDateTime referenceAt,
        List<Rank> ranks
) {
    public record Rank(
            int rank,
            Long appId
    ){}
}
