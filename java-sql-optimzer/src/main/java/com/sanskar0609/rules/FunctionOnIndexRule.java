package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

public class FunctionOnIndexRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql();
        if (sql.matches("(?i).*where\\s+\\w+\\(.*\\).*")) {
            return List.of(new Issue(
                Severity.WARN,
                "Function applied to column in WHERE — index not used",
                "Rewrite to avoid wrapping indexed columns in functions (e.g. use date ranges instead of DATE(col) = '...')",
                q.getFile(), q.getLine(), q.getSql(), -4
            ));
        }
        return List.of();
    }
}
