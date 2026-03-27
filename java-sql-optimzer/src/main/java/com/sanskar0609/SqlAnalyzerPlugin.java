package com.sanskar0609;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.sanskar0609.task.AnalyzeCodeTask;

public class SqlAnalyzerPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // Primary task name
        project.getTasks().register("analyzeSql", AnalyzeCodeTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Analyzes SQL queries in source code for performance anti-patterns");
        });
        // Back-compat alias
        project.getTasks().register("analyzeCode", AnalyzeCodeTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Alias for analyzeSql — analyzes SQL queries for performance issues");
        });
    }
}
