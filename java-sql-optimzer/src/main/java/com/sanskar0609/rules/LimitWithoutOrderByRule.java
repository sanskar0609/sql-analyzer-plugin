package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class LimitWithoutOrderByRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql().toUpperCase();
        if (sql.contains(" LIMIT ") && !sql.contains(" ORDER BY ")) {
            return List.of(new Issue(
                Severity.WARN,
                "LIMIT without ORDER BY — non-deterministic result ordering",
                "Add an ORDER BY clause when using LIMIT to ensure consistent ordering",
                q.getFile(), q.getLine(), q.getSql(), -2
            ));
        }
        return List.of();
    }
}
