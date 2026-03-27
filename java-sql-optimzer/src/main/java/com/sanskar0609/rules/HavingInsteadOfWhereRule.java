package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;
import java.util.regex.Pattern;

public class HavingInsteadOfWhereRule implements Rule {
    private static final Pattern AGG_PATTERN = Pattern.compile("(?i)(COUNT|MAX|MIN|SUM|AVG)");

    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql = q.getSql().toUpperCase();
        if (sql.contains(" HAVING ") && !AGG_PATTERN.matcher(sql).find()) {
            return List.of(new Issue(
                Severity.WARN,
                "HAVING clause used without aggregation — inefficient",
                "Move the condition from HAVING to WHERE to filter rows before grouping",
                q.getFile(), q.getLine(), q.getSql(), -4
            ));
        }
        return List.of();
    }
}
