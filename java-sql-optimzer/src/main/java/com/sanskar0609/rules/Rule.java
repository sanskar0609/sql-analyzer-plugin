package com.sanskar0609.rules;

import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Issue;
import java.util.List;

public interface Rule {
    List<Issue> check(ExtractedQuery query);
}
