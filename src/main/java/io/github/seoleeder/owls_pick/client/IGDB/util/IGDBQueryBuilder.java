package io.github.seoleeder.owls_pick.client.IGDB.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IGDBQueryBuilder {

    private final List<String> fieldList = new ArrayList<>(); // 리스트로 관리
    private final List<String> whereClauses = new ArrayList<>();
    private Integer limit;
    private String sort;

    public static IGDBQueryBuilder create() {
        return new IGDBQueryBuilder();
    }

    public IGDBQueryBuilder fields(String... fields) {
        this.fieldList.addAll(Arrays.asList(fields));
        return this;
    }

    public IGDBQueryBuilder where(String clause) {
        this.whereClauses.add(clause);
        return this;
    }

    public IGDBQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public IGDBQueryBuilder sort(String sortClause) {
        this.sort = sortClause;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();

        // 리스트에 담긴 필드들을 ", "로 연결
        if (fieldList.isEmpty()) {
            sb.append("fields *; ");
        } else {
            sb.append("fields ").append(String.join(", ", fieldList)).append("; ");
        }

        //where 절의 조건들을 & 로 연결
        if (!whereClauses.isEmpty()) {
            sb.append("where ").append(String.join(" & ", whereClauses)).append("; ");
        }

        if (sort != null) {
            sb.append("sort ").append(sort).append("; ");
        }

        if (limit != null) {
            sb.append("limit ").append(limit).append("; ");
        }

        return sb.toString().trim(); // 양 끝 공백 제거
    }
}
