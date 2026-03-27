package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class TooManyJoinsRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql       = q.getSql().toUpperCase();
        int    joinCount = sql.split("JOIN").length - 1;
        if (joinCount > 3) {
            return List.of(new Issue(
                Severity.WARN,
                "Query has too many JOINs (" + joinCount + ") — may hurt performance",
                "Consider denormalizing, caching, or splitting into smaller queries",
                q.getFile(), q.getLine(), q.getSql(), -5
            ));
        }
        return List.of();
    }
}
