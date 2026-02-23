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
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_social_account_user_id"))
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    public enum Provider{
        GOOGLE, KAKAO, NAVER
    }
}
