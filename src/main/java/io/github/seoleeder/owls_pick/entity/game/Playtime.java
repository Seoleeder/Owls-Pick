package io.github.seoleeder.owls_pick.entity.game;

import io.github.seoleeder.owls_pick.entity.game.enums.status.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "playtime")
public class Playtime {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", foreignKey = @ForeignKey(name = "fk_playtime_game_id"))
    private Game game;

    @Column
    private Integer mainStory;

    @Column
    private Integer mainExtras;

    @Column
    private Integer completionist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.UNSYNCED;

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate // 수정 전 실행
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * HLTB 스크래핑 결과 업데이트 로직
     */
    public void updateSyncResult(Integer mainStory, Integer mainExtras, Integer completionist, SyncStatus status) {
        this.mainStory = mainStory;
        this.mainExtras = mainExtras;
        this.completionist = completionist;
        this.syncStatus = status;
    }
}
