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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_detail_game_id"))
    private Game game;

    @Column(name = "store_app_id")
    private String storeAppId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StoreName storeName;

    @Column(columnDefinition = "text")
    private String url;

    @Column
    private Integer originalPrice;

    @Column
    private Integer historicalLow;

    @Column
    private Integer discountPrice;

    @Column
    private Integer discountRate;

    @Column
    private LocalDateTime expiryDate;

    @Column
    private LocalDateTime createdAt;

    @Column
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

    // 가격 정보 업데이트 메서드
    public void updatePriceInfo(Integer currentPrice, Integer originalPrice, Integer historicalLow,
                                Integer discountRate, LocalDateTime expiry, String url) {
        this.url = url;
        this.historicalLow = historicalLow;

        // 할인이 적용된 경우 (Cut > 0)
        if (discountRate != null && discountRate > 0) {
            this.originalPrice = originalPrice;
            this.discountPrice = currentPrice;
            this.discountRate = discountRate;
            this.expiryDate = expiry;
        }
        // 할인 중이 아닌 경우 (정가 판매)
        else {
            this.originalPrice = (originalPrice != null) ? originalPrice : currentPrice;

            // 할인가, 할인율, 만료 시각 초기화
            this.discountPrice = null;
            this.discountRate = 0;
            this.expiryDate = null;
        }
    }

    public enum StoreName {
        STEAM, EPIC_GAMES_STORE, UBISOFT_STORE, EA_STORE, MICROSOFT_STORE, GREEN_MAN_GAMING, GAMERSGATE, FANATICAL, BLIZZARD
    }

}
