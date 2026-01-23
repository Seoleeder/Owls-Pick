package io.github.seoleeder.owls_pick.entity.game;

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
    private int id;

    @Column(name = "main_story")
    private Integer mainStory;

    @Column(name = "main_extras")
    private Integer mainExtras;

    @Column
    private Integer completionist;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate // 수정 전 실행
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
