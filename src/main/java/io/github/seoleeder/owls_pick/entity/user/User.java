package io.github.seoleeder.owls_pick.entity.user;

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

    @Column(nullable = false)
    private String email;

    @Column
    private LocalDate birthDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean isOnboarded = false; // 온보딩 완료 여부

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
    public void completeOnboarding(java.time.LocalDate birthDate, List<String> tags, List<String> stores) {
        this.birthDate = birthDate;
        this.preferredTags = tags;
        this.preferredStores = stores;
        this.isOnboarded = true;
    }



}
