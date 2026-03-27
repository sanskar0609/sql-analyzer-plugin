package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class NPlus1Rule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql().toUpperCase();
        if (sql.contains("WHERE") && sql.contains("IN") && sql.contains("?")) {
            return List.of(new Issue(
                Severity.INFO,
                "Query might cause N+1 problem if used in a loop",
                "Batch fetch related records or use a JOIN instead of looping queries",
                q.getFile(), q.getLine(), q.getSql(), -3
            ));
        }
        return List.of();
    }
}
