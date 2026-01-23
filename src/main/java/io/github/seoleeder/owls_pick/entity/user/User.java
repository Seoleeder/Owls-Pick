package io.github.seoleeder.owls_pick.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    /** List 타입을 DB에서 이해할 수 있도록 Array타입으로 변환 */
    @Setter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_tag", columnDefinition = "text[]")
    private List<String> preferredTag;

    @Setter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_platform", columnDefinition = "text[]")
    private List<String> preferredPlatform;

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
