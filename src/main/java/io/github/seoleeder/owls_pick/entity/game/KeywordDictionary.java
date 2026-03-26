package io.github.seoleeder.owls_pick.entity.game;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "keyword_dictionary")
public class KeywordDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UNIQUE로 중복 번역 방지
    @Column(length = 255, unique = true, nullable = false)
    private String engName;

    @Column(length = 255, nullable = false)
    private String korName;

    public void updateLocalization(String korName) {
        this.korName = korName;
    }
}