package io.github.seoleeder.owls_pick.entity.game;

import io.github.seoleeder.owls_pick.entity.game.enums.AdultKeyword;
import io.github.seoleeder.owls_pick.entity.game.enums.ThemeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
public class Tag {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tag_game_id"))
    private Game game;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> genres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> themes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> keywords = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    @Column(columnDefinition = "text[]")
    private List<String> keywordsKo = new ArrayList<>();

    /**
     * 단일 게임의 태그 목록(테마, 키워드)에 성인(19금) 요소가 포함되어 있는지 판별
     */
    public boolean isAdult() {
        // 1. 테마 배열 검사
        if (this.themes != null && !this.themes.isEmpty()) {
            boolean hasAdultTheme = this.themes.stream()
                    .anyMatch(themeStr -> {
                        try {
                            // DB의 String을 ThemeType Enum으로 변환 후 isAdult() 호출
                            return ThemeType.valueOf(themeStr).isAdult();
                        } catch (IllegalArgumentException e) {
                            return false; // 매핑되지 않는 예외적인 테마명일 경우 안전하게 무시
                        }
                    });
            if (hasAdultTheme) return true;
        }

        // 2. 키워드 배열 검사
        if (this.keywords != null && !this.keywords.isEmpty()) {
            List<String> adultKeywordNames = AdultKeyword.getAllNames();
            boolean hasAdultKeyword = this.keywords.stream()
                    .anyMatch(keywordStr -> adultKeywordNames.contains(keywordStr.toLowerCase()));
            if (hasAdultKeyword) return true;
        }

        // 장르는 성인용이 없으므로 검사 생략
        return false;
    }

    /**
     * 한글화된 키워드 배열 업데이트
     */
    public void updateKeywordsKo(List<String> translatedKeywords) {
        this.keywordsKo = translatedKeywords;
    }

}
