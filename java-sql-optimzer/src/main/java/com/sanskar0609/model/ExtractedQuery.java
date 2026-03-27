package com.sanskar0609.model;

import java.io.File;

public class ExtractedQuery {
    private final String sql;
    private final int line;
    private final File file;

    public ExtractedQuery(String sql, int line, File file) {
        this.sql = sql;
        this.line = line;
        this.file = file;
    }

    public String getSql() { return sql; }
    public int getLine() { return line; }
    public File getFile() { return file; }
}
