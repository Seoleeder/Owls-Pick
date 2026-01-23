package io.github.seoleeder.owls_pick.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
// 외부에서 무분별하게 빈 객체를 생성하지 못하도록 제한
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_account")
public class SocialAccount {

    @Id
    private int id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

}
