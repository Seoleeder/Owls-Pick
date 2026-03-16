package io.github.seoleeder.owls_pick.entity.user;

import io.github.seoleeder.owls_pick.dto.request.ProfileUpdateRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 30, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Column
    private LocalDate birthDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean isOnboarded = false; // 온보딩 완료 여부

    // 할인 푸시 알림 수신 동의 여부
    @Builder.Default
    @Column(nullable = false)
    private boolean isDiscountNotificationEnabled = false;

    /** List 타입을 DB에서 이해할 수 있도록 Array타입으로 변환 */
    @Setter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredTags;

    @Setter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> preferredStores;

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



    // ------ 온보딩용 메서드 ------

    /**
     * 성인 여부 계산
     * */
    public boolean isAdultUser() {
        if (this.birthDate == null) return false;
        int currentYear = LocalDate.now().getYear();
        int birthYear = this.birthDate.getYear();

        // 현재 연도 - 태어난 연도가 19 이상이면 무조건 성인
        return (currentYear - birthYear) >= 19;
    }

    /**
     * 온보딩 정보 업데이트
     */
    public void completeOnboarding(String nickname, LocalDate birthDate, List<String> tags, List<String> stores) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.preferredTags = tags;
        this.preferredStores = stores;
        this.isOnboarded = true;
    }


    // ------ 회원 정보 수정용 메서드 ------

    /**
     * 프로필 정보 선택적 업데이트 (닉네임, 선호 태그 및 스토어)
     * */
    public void updateProfile(ProfileUpdateRequest request) {
        if (request.nickname() != null) this.nickname = request.nickname();
        if (request.preferredTags() != null) this.preferredTags = request.preferredTags();
        if (request.preferredStores() != null) this.preferredStores = request.preferredStores();
    }

    /**
     * 할인 알림 설정 토글 (ON/OFF)
     */
    public void updateDiscountNotification(Boolean isEnabled) {
        if (isEnabled != null) {
            this.isDiscountNotificationEnabled = isEnabled;
        }
    }

}
