package com.sanskar0609.rules;

import com.sanskar0609.model.AnalysisResult;
import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleEngine {

    private final List<Rule> rules = List.of(
        new SelectStarRule(),
        new WhereAndLimitRule(),           // 👈 replaces MissingWhereRule + NoLimitRule
        new CartesianJoinRule(),
        new NPlus1Rule(),
        new TooManyJoinsRule(),
        new LeadingWildcardRule(),
        new MultipleOrRule(),
        new SubqueryInRule(),
        new NotInRule(),
        new FunctionOnIndexRule(),
        new HavingInsteadOfWhereRule(),
        new RedundantDistinctRule(),
        new LimitWithoutOrderByRule(),
        new OrderByIndexSuggestionRule()   // 👈 new index suggestion rule
    );

    public AnalysisResult analyze(List<ExtractedQuery> queries) {
        List<Issue> issues = new ArrayList<>();
        for (ExtractedQuery q : queries) {
            for (Rule rule : rules) {
                issues.addAll(rule.check(q));
            }
        }
        return new AnalysisResult(groupDuplicates(issues), queries.size());
    }

    /**
     * Groups issues that share the same message+file into a single summarised issue.
     *
     * Example: if "SELECT * fetches all columns" fires 8 times in App.java,
     * they are collapsed to one issue:
     *   "SELECT * fetches all columns — impacts performance (8 occurrences in App.java)"
     *
     * The first occurrence's SQL snippet, severity, weight, and location are preserved.
     * For cross-file duplicates, each file gets its own grouped entry.
     */
    private List<Issue> groupDuplicates(List<Issue> raw) {
        // Key = message + "|" + file absolute path
        Map<String, List<Issue>> groups = new LinkedHashMap<>();
        for (Issue issue : raw) {
            String key = issue.getMessage() + "|" + issue.getFile().getAbsolutePath();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(issue);
        }

        List<Issue> result = new ArrayList<>();
        for (List<Issue> group : groups.values()) {
            if (group.size() == 1) {
                result.add(group.get(0));
            } else {
                Issue first = group.get(0);
                int   count = group.size();
                // Accumulate total weight (multiply by occurrences, capped for fairness)
                int totalWeight = Math.max(
                    group.stream().mapToInt(Issue::getWeight).sum(),
                    first.getWeight() * 3  // cap at 3× the base weight
                );
                String groupedMsg = first.getMessage()
                        + " (" + count + " occurrences in " + first.getFile().getName() + ")";
                result.add(new Issue(
                    first.getSeverity(),
                    groupedMsg,
                    first.getSuggestion(),
                    first.getFile(),
                    first.getLine(),
                    first.getSql(),      // show first occurrence's SQL
                    totalWeight
                ));
            }
        }
        return result;
    }
}
