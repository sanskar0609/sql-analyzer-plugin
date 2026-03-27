package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class SelectStarRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql    = q.getSql().toLowerCase();
        String rawSql = q.getSql();

        if (sql.contains("select *") && sql.contains("join")) {
            return List.of(new Issue(
                Severity.WARN,
                "Avoid SELECT * in JOIN queries — fetches redundant columns",
                "Specify only required columns: SELECT t.id, t.name",
                q.getFile(), q.getLine(), rawSql, -8
            ));
        } else if (sql.contains("select *")) {
            return List.of(new Issue(
                Severity.WARN,
                "SELECT * fetches all columns — impacts performance",
                "Specify only required columns: SELECT id, name, email",
                q.getFile(), q.getLine(), rawSql, -5
            ));
        }
        return List.of();
    }
}
