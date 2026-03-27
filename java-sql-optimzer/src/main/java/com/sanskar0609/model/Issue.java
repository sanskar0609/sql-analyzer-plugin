package com.sanskar0609.model;

import java.io.File;

public class Issue {
    private final Severity severity;
    private final String   message;
    private final String   suggestion;
    private final File     file;
    private final int      line;
    private final String   sql;      // SQL snippet for context
    private final int      weight;   // score deduction (negative value, e.g. -15)

    public Issue(Severity severity, String message, String suggestion,
                 File file, int line, String sql, int weight) {
        this.severity   = severity;
        this.message    = message;
        this.suggestion = suggestion;
        this.file       = file;
        this.line       = line;
        this.sql        = sql;
        this.weight     = weight;
    }

    public Severity getSeverity()   { return severity;   }
    public String   getMessage()    { return message;    }
    public String   getSuggestion() { return suggestion; }
    public File     getFile()       { return file;       }
    public int      getLine()       { return line;       }
    public String   getSql()        { return sql;        }
    /** Score deduction — always a negative integer, e.g. -15 */
    public int      getWeight()     { return weight;     }
}
