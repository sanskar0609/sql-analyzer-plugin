package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class MultipleOrRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql();
        if (sql.matches("(?i).*\\bOR\\b.*\\bOR\\b.*")) {
            return List.of(new Issue(
                Severity.WARN,
                "Multiple OR conditions — consider using IN",
                "Replace multiple OR conditions on the same column with an IN clause",
                q.getFile(), q.getLine(), q.getSql(), -3
            ));
        }
        return List.of();
    }
}
