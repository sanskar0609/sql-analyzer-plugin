package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class SubqueryInRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        if (q.getSql().toLowerCase().contains(" in (select")) {
            return List.of(new Issue(
                Severity.WARN,
                "Subquery in IN clause — potential performance issue",
                "Use a JOIN or EXISTS clause instead of IN (SELECT...) for better performance",
                q.getFile(), q.getLine(), q.getSql(), -5
            ));
        }
        return List.of();
    }
}
