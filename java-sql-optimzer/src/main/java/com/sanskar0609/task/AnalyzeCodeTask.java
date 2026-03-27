package com.sanskar0609.task;

import com.github.javaparser.ast.CompilationUnit;
import com.sanskar0609.config.AnalyzerConfig;
import com.sanskar0609.config.ConfigLoader;
import com.sanskar0609.extractor.SqlQueryExtractor;
import com.sanskar0609.llm.LLMEngine;
import com.sanskar0609.model.AnalysisResult;
import com.sanskar0609.model.ExtractedQuery;
import com.sanskar0609.model.Severity;
import com.sanskar0609.report.ReportGenerator;
import com.sanskar0609.rules.RuleEngine;
import com.sanskar0609.scanner.CodeScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.List;

public class AnalyzeCodeTask extends DefaultTask {

    @TaskAction
    public void analyze() {
        File sourceDir = getProject().getProjectDir(); // Basic representation, might need to be specific to src/
        AnalyzerConfig config = new ConfigLoader().load(getProject().getProjectDir());
        
        List<CompilationUnit> units = new CodeScanner().scan(sourceDir);
        List<ExtractedQuery> queries = new SqlQueryExtractor().extract(units);
        AnalysisResult result = new RuleEngine().analyze(queries);

        if (config.isEnableLLM()) {
            new LLMEngine().enrichResults(result);
        }

        File buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile();
        new ReportGenerator().generate(result, config.getOutputFormat(), buildDir);

        long errorCount = result.getIssues().stream()
            .filter(i -> i.getSeverity() == Severity.ERROR).count();

        if (config.isFailOnError() && errorCount > 0) {
            throw new GradleException(
                errorCount + " SQL error(s) found. Fix before building.");
        }
    }
}
