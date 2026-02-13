package io.github.seoleeder.owls_pick.entity.user;

import io.github.seoleeder.owls_pick.entity.game.Game;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wishlist")
public class Wishlist {

    @EmbeddedId
    private WishlistId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wishlist_user_id"))
    private User user;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wishlist_game_id"))
    private Game game;

    @Getter
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist // 저장 전 시각 자동 기록
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
