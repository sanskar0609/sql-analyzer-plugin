package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class CartesianJoinRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql();
        if (sql.contains(",") && !sql.toLowerCase().contains("join")) {
            return List.of(new Issue(
                Severity.ERROR,
                "Potential Cartesian Join detected — all row combinations returned",
                "Use explicit JOIN with an ON or USING clause instead of comma-separated tables",
                q.getFile(), q.getLine(), q.getSql(), -20
            ));
        }
        return List.of();
    }
}
