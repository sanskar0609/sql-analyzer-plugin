package com.sanskar0609.model;

import java.util.List;

public class AnalysisResult {
    private final List<Issue> issues;
    private final int totalQueries;

    public AnalysisResult(List<Issue> issues, int totalQueries) {
        this.issues       = issues;
        this.totalQueries = totalQueries;
    }

    public List<Issue> getIssues()    { return issues;       }
    public int getTotalQueries()      { return totalQueries; }

    /** Score = 100 + sum of all issue weights (weights are negative). Clamped to [0, 100]. */
    public int getScore() {
        int score = 100;
        for (Issue issue : issues) {
            score += issue.getWeight(); // weight is already negative
        }
        return Math.max(0, Math.min(100, score));
    }
}
