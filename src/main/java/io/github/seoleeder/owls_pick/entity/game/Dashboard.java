package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "dashboard")
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dashboard_game_id"))
    private Game game;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurationType curationType;

    @Column
    private int rank;

    @Column
    private LocalDateTime referenceAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate // 수정 전 실행
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum CurationType {
        WEEKLY_TOP_SELLER, MONTHLY_TOP, YEARLY_TOP, CONCURRENT_PLAYER, MOST_PLAYED
    }


}
