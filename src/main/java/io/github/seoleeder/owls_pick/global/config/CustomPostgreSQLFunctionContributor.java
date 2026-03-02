package io.github.seoleeder.owls_pick.global.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

public class CustomPostgreSQLFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        // array_contains(컬럼, 값) -> 컬럼 @> CAST(ARRAY[값] AS text[])
        functionContributions.getFunctionRegistry()
                .registerPattern("array_contains", "?1 @> CAST(ARRAY[?2] AS text[])");
    }
}
