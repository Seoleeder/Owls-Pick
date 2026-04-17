package io.github.seoleeder.owls_pick.repository.support;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import io.github.seoleeder.owls_pick.entity.game.QGame;
import org.springframework.stereotype.Component;

import static io.github.seoleeder.owls_pick.entity.game.QTag.tag;

@Component
public class LocalizationExpressions {
    /**
     * 설명(Description) 한글화가 필요한지 여부 반환
     * (원본 영문 설명은 존재하나, 한글화된 데이터가 X)
     */
    public BooleanExpression needsDescriptionLocalization(QGame game) {
        BooleanExpression hasOriginal = game.description.isNotNull().and(game.description.isNotEmpty());
        BooleanExpression isNotTranslated = game.descriptionKo.isNull().or(game.descriptionKo.isEmpty());
        return hasOriginal.and(isNotTranslated);
    }

    /**
     * 스토리라인(Storyline) 한글화가 필요한지 여부 반환
     * (원본 영문 스토리라인은 존재하나, 한글화된 데이터가 X)
     */
    public BooleanExpression needsStorylineLocalization(QGame game) {
        BooleanExpression hasOriginal = game.storyline.isNotNull().and(game.storyline.isNotEmpty());
        BooleanExpression isNotTranslated = game.storylineKo.isNull().or(game.storylineKo.isEmpty());
        return hasOriginal.and(isNotTranslated);
    }

    /**
     * 영문 키워드(keywords) 배열에 데이터가 존재하는지 확인
     * (PostgreSQL Array 타입의 isEmpty() 에러 방지를 위해 cardinality 네이티브 함수 사용)
     */
    public BooleanExpression hasKeywords() {
        return Expressions.booleanTemplate("function('cardinality', {0}) > 0", tag.keywords);
    }

    /**
     * 영문 키워드(keywords) 배열의 크기(길이)를 반환하는 템플릿
     */
    private NumberTemplate<Integer> getKeywordsSize() {
        return Expressions.numberTemplate(Integer.class, "function('cardinality', {0})", tag.keywords);
    }

    /**
     * 한글 키워드(keywordsKo) 배열의 크기(길이)를 반환하는 템플릿
     */
    private NumberTemplate<Integer> getKeywordsKoSize() {
        return Expressions.numberTemplate(Integer.class, "function('cardinality', {0})", tag.keywordsKo);
    }

    /**
     * 키워드(Tag Keywords) 한글화가 필요한지 여부 반환
     * 1. 영문 키워드 존재
     * 2. 한글 키워드 배열이 비어있거나(0), 영문 키워드 개수와 불일치
     */
    public BooleanExpression needsKeywordLocalization() {
        return hasKeywords().and(
                getKeywordsKoSize().isNull()
                        .or(getKeywordsKoSize().ne(getKeywordsSize()))
        );
    }
}
