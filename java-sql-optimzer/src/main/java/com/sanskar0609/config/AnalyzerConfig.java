package com.sanskar0609.config;

public class AnalyzerConfig {
    private final boolean enableLLM;
    private final boolean failOnError;
    private final int maxWarnings;
    private final String outputFormat;

    public AnalyzerConfig(boolean enableLLM, boolean failOnError, int maxWarnings, String outputFormat) {
        this.enableLLM = enableLLM;
        this.failOnError = failOnError;
        this.maxWarnings = maxWarnings;
        this.outputFormat = outputFormat;
    }

    public static AnalyzerConfig defaults() {
        return new AnalyzerConfig(false, false, 10, "cli");
    }

    public boolean isEnableLLM() { return enableLLM; }
    public boolean isFailOnError() { return failOnError; }
    public int getMaxWarnings() { return maxWarnings; }
    public String getOutputFormat() { return outputFormat; }
}
