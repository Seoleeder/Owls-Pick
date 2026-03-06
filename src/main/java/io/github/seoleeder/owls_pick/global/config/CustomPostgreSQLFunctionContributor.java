package io.github.seoleeder.owls_pick.global.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

/**
 * PostgreSQL 전용 연산자를 Hibernate 에서 사용하기 위해 커스텀 함수를 등록하는 클래스
 * QueryDSL에서 function()로 호출 가능
 * */
public class CustomPostgreSQLFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();

        /*
         * array_contains: 배열 컬럼에 특정 단일 값이 포함되어 있는지 확인
         * 컬럼 @> CAST(ARRAY['값'] AS text[])
         */
        registry.registerPattern("array_contains", "?1 @> CAST(ARRAY[?2] AS text[])");

        /*
         * array_overlap: 두 배열 간에 겹치는 요소가 하나라도 있는지 확인 (교집합 존재 여부)
         * 컬럼 && 배열
         */
        registry.registerPattern("array_overlap", "(?1 && ?2)");

    }
}
