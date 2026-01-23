package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store_detail")
public class StoreDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "store_app_id", nullable = false)
    private int storeAppId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private String name;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(columnDefinition = "text")
    private String url;

    @Column(name = "base_price")
    private int basePrice;

    @Column(name = "discount_price")
    private int discountPrice;

    @Column(name = "discount_rate")
    private int discountRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist // 저장 전 실행
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate // 수정 전 실행
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
