package com.sanskar0609.maven;

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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * Maven Mojo for the SQL Analyzer.
 *
 * Usage in pom.xml:
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;com.sanskar0609&lt;/groupId&gt;
 *   &lt;artifactId&gt;sql-analyzer-maven-plugin&lt;/artifactId&gt;
 *   &lt;version&gt;1.0.5&lt;/version&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;goals&gt;&lt;goal&gt;analyze&lt;/goal&gt;&lt;/goals&gt;
 *     &lt;/execution&gt;
 *   &lt;/executions&gt;
 *   &lt;!-- Optional configuration --&gt;
 *   &lt;configuration&gt;
 *     &lt;failOnError&gt;true&lt;/failOnError&gt;
 *     &lt;outputFormat&gt;html&lt;/outputFormat&gt;
 *   &lt;/configuration&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * Or run directly:
 * <pre>
 *   mvn sql-analyzer:analyze
 * </pre>
 */
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class AnalyzeSqlMojo extends AbstractMojo {

    // ── Parameters ─────────────────────────────────────────────────────────

    /** Root directory of the Maven project (auto-injected). */
    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File baseDir;

    /** Target/build directory for report output (auto-injected). */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDir;

    /**
     * Fail the build when at least one ERROR-severity issue is found.
     * Equivalent to Gradle's {@code failOnError}.
     */
    @Parameter(defaultValue = "false", property = "sql.failOnError")
    private boolean failOnError;

    /**
     * Report output format: {@code html} (default), {@code json}, or {@code cli}.
     * "html" always generates the dashboard; "json" also writes report.json.
     */
    @Parameter(defaultValue = "html", property = "sql.outputFormat")
    private String outputFormat;

    /**
     * Enable experimental LLM enrichment of analysis results.
     */
    @Parameter(defaultValue = "false", property = "sql.enableLLM")
    private boolean enableLLM;

    // ── Execute ────────────────────────────────────────────────────────────

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLog().info("  SQL Analyzer Maven Plugin v1.0.5");
        getLog().info("  Scanning: " + baseDir.getAbsolutePath());
        getLog().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Load configuration from sql-analyzer.yml (or defaults if not present)
        AnalyzerConfig config = buildConfig();

        // Scan → Extract → Analyze
        List<CompilationUnit> units   = new CodeScanner().scan(baseDir);
        List<ExtractedQuery>  queries = new SqlQueryExtractor().extract(units);
        AnalysisResult        result  = new RuleEngine().analyze(queries);

        getLog().info("Queries found: " + result.getTotalQueries());
        getLog().info("Issues found:  " + result.getIssues().size());

        // Optional LLM enrichment
        if (config.isEnableLLM()) {
            getLog().info("LLM enrichment enabled — enriching results...");
            new LLMEngine().enrichResults(result);
        }

        // Generate report (HTML dashboard + optional JSON)
        new ReportGenerator().generate(result, config.getOutputFormat(), buildDir);

        // Fail build on errors?
        long errorCount = result.getIssues().stream()
                .filter(i -> i.getSeverity() == Severity.ERROR)
                .count();

        if (config.isFailOnError() && errorCount > 0) {
            throw new MojoFailureException(
                errorCount + " SQL error(s) detected. Fix before building. "
                + "Set <failOnError>false</failOnError> to suppress.");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Build an {@link AnalyzerConfig} — first tries to load {@code sql-analyzer.yml}
     * from the project root, then applies any pom.xml {@code <configuration>} overrides.
     */
    private AnalyzerConfig buildConfig() {
        // Start from YAML file if it exists
        AnalyzerConfig yaml = new ConfigLoader().load(baseDir);

        // pom.xml params override YAML values (explicit > implicit)
        return new AnalyzerConfig(
            enableLLM    || yaml.isEnableLLM(),
            failOnError  || yaml.isFailOnError(),
            yaml.getMaxWarnings(),
            outputFormat != null ? outputFormat : yaml.getOutputFormat()
        );
    }
}
