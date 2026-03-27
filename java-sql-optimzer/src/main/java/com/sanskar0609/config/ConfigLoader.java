package com.sanskar0609.config;

import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class ConfigLoader {
    public AnalyzerConfig load(File projectDir) {
        File configFile = new File(projectDir, "sql-analyzer.yml");
        if (!configFile.exists()) {
            return AnalyzerConfig.defaults();
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(reader);
            if (data == null || !data.containsKey("sqlAnalyzer")) {
                return AnalyzerConfig.defaults();
            }
            Map<String, Object> config = (Map<String, Object>) data.get("sqlAnalyzer");
            
            return new AnalyzerConfig(
                (boolean) config.getOrDefault("enableLLM", false),
                (boolean) config.getOrDefault("failOnError", false),
                (int) config.getOrDefault("maxWarnings", 10),
                (String) config.getOrDefault("outputFormat", "cli")
            );
        } catch (IOException e) {
            e.printStackTrace();
            return AnalyzerConfig.defaults();
        }
    }
}
