package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects ORDER BY clauses and suggests adding an index on the sorted column.
 *
 * Example trigger: SELECT * FROM orders ORDER BY created_at
 * Suggestion:      Consider adding an index on column: created_at
 */
public class OrderByIndexSuggestionRule implements Rule {

    // Captures the first column name after ORDER BY (ignores ASC/DESC)
    private static final Pattern ORDER_BY_PATTERN =
            Pattern.compile("(?i)\\bORDER\\s+BY\\s+([\\w.]+)");

    @Override
    public List<Issue> check(ExtractedQuery q) {
        List<Issue> issues = new ArrayList<>();
        String sql    = q.getSql();
        String sqlUp  = sql.toUpperCase();

        if (!sqlUp.contains("ORDER BY")) return issues;

        Matcher m = ORDER_BY_PATTERN.matcher(sql);
        while (m.find()) {
            String col = m.group(1);
            issues.add(new Issue(
                Severity.INFO,
                "ORDER BY on column '" + col + "' — ensure an index exists",
                "Consider adding an index: CREATE INDEX idx_" + col.replace(".", "_")
                        + " ON <table>(" + col + ")",
                q.getFile(), q.getLine(), sql, -1
            ));
        }
        return issues;
    }
}
