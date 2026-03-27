package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class LeadingWildcardRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql().toUpperCase();
        if (sql.matches(".*\\bLIKE\\s+'%.*")) {
            return List.of(new Issue(
                Severity.WARN,
                "Leading wildcard in LIKE '%...' — prevents index usage",
                "Remove the leading '%' or use full-text search (MATCH...AGAINST)",
                q.getFile(), q.getLine(), q.getSql(), -5
            ));
        }
        return List.of();
    }
}
