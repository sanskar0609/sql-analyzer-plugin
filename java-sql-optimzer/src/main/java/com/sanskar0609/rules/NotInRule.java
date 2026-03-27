package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class NotInRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        if (q.getSql().toLowerCase().contains("not in")) {
            return List.of(new Issue(
                Severity.WARN,
                "NOT IN detected — dangerous NULL handling",
                "Use NOT EXISTS instead to avoid unexpected behavior with NULL values",
                q.getFile(), q.getLine(), q.getSql(), -3
            ));
        }
        return List.of();
    }
}
