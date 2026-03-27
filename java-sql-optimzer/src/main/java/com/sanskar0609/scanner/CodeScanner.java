package com.sanskar0609.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CodeScanner {
    public List<CompilationUnit> scan(File sourceDir) {
        List<CompilationUnit> units = new ArrayList<>();
        JavaParser javaParser = new JavaParser();
        
        try {
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                Files.walk(sourceDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            ParseResult<CompilationUnit> result = javaParser.parse(path);
                            result.getResult().ifPresent(units::add);
                        } catch (IOException e) {
                            // ignore parse errors for individual files
                        }
                    });
            }
        } catch (IOException e) { 
            e.printStackTrace();
        }
        return units;
    }
}
