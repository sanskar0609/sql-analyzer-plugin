package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;
import java.util.List;

/**
 * Smart merged rule — replaces both MissingWhereRule and NoLimitRule.
 *
 * Logic:
 *   • SELECT/UPDATE/DELETE with no WHERE  →  WARN  (−5)
 *   • SELECT with no WHERE AND no LIMIT   →  ERROR (−15) [escalates, replaces the WARN]
 *   • DELETE/UPDATE with no WHERE         →  ERROR (−25)
 *
 * Only ONE issue is emitted per query, eliminating duplicate messages.
 */
public class WhereAndLimitRule implements Rule {

    @Override
    public List<Issue> check(ExtractedQuery q) {
        String sql    = q.getSql().toUpperCase();
        String rawSql = q.getSql();

        boolean isSelect = sql.contains("SELECT");
        boolean isDelete = sql.contains("DELETE");
        boolean isUpdate = sql.contains("UPDATE");
        boolean hasWhere = sql.contains("WHERE");
        boolean hasLimit = sql.contains("LIMIT");

        if (!hasWhere) {
            // DELETE / UPDATE without WHERE — most dangerous
            if (isDelete || isUpdate) {
                String op = isDelete ? "DELETE" : "UPDATE";
                return List.of(new Issue(
                    Severity.ERROR,
                    op + " statement without WHERE — affects ALL rows",
                    "Add a WHERE clause to restrict which rows are modified",
                    q.getFile(), q.getLine(), rawSql, -25
                ));
            }

            // SELECT with no WHERE AND no LIMIT — unbounded full-table scan
            if (isSelect && !hasLimit) {
                return List.of(new Issue(
                    Severity.ERROR,
                    "No WHERE and no LIMIT — unbounded full-table scan",
                    "Add a WHERE condition and/or a LIMIT to restrict rows returned",
                    q.getFile(), q.getLine(), rawSql, -15
                ));
            }

            // SELECT with no WHERE but has LIMIT — warn only
            if (isSelect) {
                return List.of(new Issue(
                    Severity.WARN,
                    "Query has no WHERE clause — full table scan risk",
                    "Add a WHERE condition to limit rows scanned",
                    q.getFile(), q.getLine(), rawSql, -5
                ));
            }
        }

        return List.of();
    }
}
