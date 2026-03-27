package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import java.util.List;

/**
 * Replaced by WhereAndLimitRule — kept as empty stub so any external
 * code that still references this class compiles without error.
 * @deprecated Use WhereAndLimitRule instead.
 */
@Deprecated
public class NoLimitRule implements Rule {
    @Override
    public List<Issue> check(ExtractedQuery q) {
        return List.of(); // no-op — logic moved to WhereAndLimitRule
    }
}
