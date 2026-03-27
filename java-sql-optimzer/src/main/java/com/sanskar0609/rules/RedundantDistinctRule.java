package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class RedundantDistinctRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql().toUpperCase();
        if (sql.contains("SELECT DISTINCT") && sql.contains(" GROUP BY ")) {
            return List.of(new Issue(
                Severity.INFO,
                "Redundant DISTINCT with GROUP BY — unnecessary overhead",
                "Remove DISTINCT; GROUP BY already ensures uniqueness for the grouped columns",
                q.getFile(), q.getLine(), q.getSql(), -2
            ));
        }
        return List.of();
    }
}
