package com.sanskar0609.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanskar0609.model.AnalysisResult;
import com.sanskar0609.model.Issue;
import com.sanskar0609.model.Severity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGenerator {

    public void generate(AnalysisResult result, String format, File buildDir) {
        printCLI(result);
        writeHTML(result, buildDir);

        if ("json".equalsIgnoreCase(format)) {
            writeJSON(result, buildDir);
        }
    }

    // ─── CLI output ───────────────────────────────────────────────────────────

    private void printCLI(AnalysisResult result) {
        result.getIssues().forEach(issue -> {
            String prefix = issue.getSeverity() == Severity.ERROR ? "[ERROR]"
                          : issue.getSeverity() == Severity.WARN  ? "[ WARN]"
                          :                                         "[ INFO]";
            System.out.println(prefix + " " + issue.getMessage());
            System.out.println("  File: " + issue.getFile().getAbsolutePath() + ":" + issue.getLine());
            if (issue.getSql() != null && !issue.getSql().isBlank()) {
                String snippet = issue.getSql().trim().replaceAll("\\s+", " ");
                if (snippet.length() > 120) snippet = snippet.substring(0, 117) + "...";
                System.out.println("  SQL:  " + snippet);
            }
            System.out.println("  Fix:  " + issue.getSuggestion());
            System.out.println();
        });

        int score = result.getScore();
        String rating = rating(score);
        System.out.println("==========================================");
        System.out.println("        SQL CODE HEALTH: " + score + " / 100");
        System.out.println("              Rating: " + rating);
        System.out.println("==========================================");
        System.out.println("  Total Queries Analyzed : " + result.getTotalQueries());
        System.out.println("  Issues Found           : " + result.getIssues().size());
        System.out.println("  Errors                 : " + count(result, Severity.ERROR));
        System.out.println("  Warnings               : " + count(result, Severity.WARN));
        System.out.println("==========================================");
    }

    // ─── HTML dashboard ───────────────────────────────────────────────────────

    private void writeHTML(AnalysisResult result, File buildDir) {
        try {
            File reportDir = new File(buildDir, "reports/sql-analyzer");
            if (!reportDir.exists()) reportDir.mkdirs();
            File htmlFile = new File(reportDir, "index.html");

            Files.write(htmlFile.toPath(), buildHTML(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("======> HTML Dashboard: " + htmlFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write HTML report: " + e.getMessage());
        }
    }

    private String buildHTML(AnalysisResult result) {
        int score      = result.getScore();
        int errors     = count(result, Severity.ERROR);
        int warnings   = count(result, Severity.WARN);
        int info       = count(result, Severity.INFO);
        int total      = result.getTotalQueries();
        String rating  = rating(score);
        String ts      = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // scoreColor: green ≥90, yellow ≥75, orange ≥50, red otherwise
        String scoreColor = score >= 90 ? "#22c55e" : score >= 75 ? "#eab308" : score >= 50 ? "#f97316" : "#ef4444";

        // dash-offset for SVG ring (circumference ~339.29 for r=54)
        double circumference = 2 * Math.PI * 54;
        double offset = circumference - (score / 100.0) * circumference;

        // Per-rule breakdown
        Map<String, Long> ruleCount = result.getIssues().stream()
                .collect(Collectors.groupingBy(Issue::getMessage, Collectors.counting()));

        // Rule rows sorted by count descending
        StringBuilder ruleRows = new StringBuilder();
        ruleCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    int pct = (int) Math.round(e.getValue() * 100.0 / Math.max(result.getIssues().size(), 1));
                    ruleRows.append("<tr>")
                            .append("<td>").append(esc(e.getKey())).append("</td>")
                            .append("<td style='width:160px'>")
                            .append("<div class='bar-wrap'><div class='bar-fill' style='width:").append(pct).append("%'></div></div>")
                            .append("</td>")
                            .append("<td class='num'>").append(e.getValue()).append("</td>")
                            .append("</tr>");
                });

        // Issues table rows
        StringBuilder issueRows = new StringBuilder();
        List<Issue> issues = result.getIssues();
        for (Issue iss : issues) {
            String sev    = iss.getSeverity().name();
            String sevCls = "sev-" + sev;
            String sqlPrev = iss.getSql() == null ? "" : iss.getSql().trim().replaceAll("\\s+", " ");
            if (sqlPrev.length() > 80) sqlPrev = sqlPrev.substring(0, 77) + "...";
            issueRows.append("<tr class='issue-row' data-sev='").append(sev).append("'>")
                     .append("<td><span class='badge ").append(sevCls).append("'>").append(sev).append("</span></td>")
                     .append("<td>").append(esc(iss.getMessage())).append("</td>")
                     .append("<td class='loc'>")
                        .append("<span class='file-name'>").append(esc(iss.getFile().getName())).append("</span>")
                        .append("<span class='line-num'>:").append(iss.getLine()).append("</span>")
                     .append("</td>")
                     .append("<td><code class='sql-snippet'>").append(esc(sqlPrev)).append("</code></td>")
                     .append("<td class='fix'>").append(esc(iss.getSuggestion())).append("</td>")
                     .append("</tr>");
        }

        // Donut chart segments (SVG) for errors/warnings/info
        // Circle circumference for r=40 → ~251.33
        double dc = 2 * Math.PI * 40;
        int totalIssues = errors + warnings + info;
        double errSeg  = totalIssues > 0 ? (errors   / (double) totalIssues) * dc : 0;
        double warnSeg = totalIssues > 0 ? (warnings / (double) totalIssues) * dc : 0;
        double infoSeg = totalIssues > 0 ? (info     / (double) totalIssues) * dc : 0;

        // SVG dasharray / dashoffset trick (rotate -90deg on <g>)
        double errOff  = 0;
        double warnOff = dc - errSeg;
        double infoOff = dc - errSeg - warnSeg;

        return "<!DOCTYPE html>\n" +
        "<html lang='en'>\n" +
        "<head>\n" +
        "  <meta charset='UTF-8'>\n" +
        "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
        "  <title>SQL Analyzer Dashboard</title>\n" +
        "  <link rel='preconnect' href='https://fonts.googleapis.com'>\n" +
        "  <link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'>\n" +
        "  <style>\n" +
        cssBlock(scoreColor, circumference, offset) +
        "  </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "  <div class='layout'>\n" +
        "    <!-- ═══ SIDEBAR ═══ -->\n" +
        "    <aside class='sidebar'>\n" +
        "      <div class='brand'>\n" +
        "        <span class='brand-icon'>⚡</span>\n" +
        "        <span class='brand-text'>SQL<strong>Lens</strong></span>\n" +
        "      </div>\n" +
        "      <nav class='nav'>\n" +
        "        <a href='#overview'   class='nav-item active'>📊 Overview</a>\n" +
        "        <a href='#breakdown'  class='nav-item'>📋 Rule Breakdown</a>\n" +
        "        <a href='#issues'     class='nav-item'>🔍 Issues</a>\n" +
        "      </nav>\n" +
        "      <div class='ts'>Generated<br><span>" + ts + "</span></div>\n" +
        "    </aside>\n" +
        "\n" +
        "    <!-- ═══ MAIN ═══ -->\n" +
        "    <main class='main'>\n" +
        "      <header class='page-header'>\n" +
        "        <div>\n" +
        "          <h1>SQL Performance Dashboard</h1>\n" +
        "          <p class='subtitle'>Automated code-level SQL anti-pattern detection</p>\n" +
        "        </div>\n" +
        "        <div class='header-badge'>" + rating + "</div>\n" +
        "      </header>\n" +
        "\n" +
        "      <!-- ── Section: Overview ── -->\n" +
        "      <section id='overview'>\n" +
        "        <div class='kpi-grid'>\n" +
        "          <!-- Score Ring -->\n" +
        "          <div class='kpi-card score-card'>\n" +
        "            <p class='kpi-label'>Health Score</p>\n" +
        "            <svg class='ring' viewBox='0 0 120 120'>\n" +
        "              <circle class='ring-bg' cx='60' cy='60' r='54'/>\n" +
        "              <circle class='ring-fill' cx='60' cy='60' r='54'\n" +
        "                      stroke-dasharray='" + fmt(circumference) + "'\n" +
        "                      stroke-dashoffset='" + fmt(offset) + "'\n" +
        "                      stroke='" + scoreColor + "'/>\n" +
        "              <text x='60' y='56' class='ring-num'>" + score + "</text>\n" +
        "              <text x='60' y='72' class='ring-sub'>/ 100</text>\n" +
        "            </svg>\n" +
        "          </div>\n" +
        "          <!-- Stats -->\n" +
        "          <div class='kpi-card stat-card'><p class='kpi-label'>Queries Scanned</p><div class='big-num'>" + total + "</div></div>\n" +
        "          <div class='kpi-card stat-card'><p class='kpi-label'>Total Issues</p><div class='big-num issues-color'>" + result.getIssues().size() + "</div></div>\n" +
        "          <div class='kpi-card stat-card'><p class='kpi-label'>Errors</p><div class='big-num err-color'>" + errors + "</div></div>\n" +
        "          <div class='kpi-card stat-card'><p class='kpi-label'>Warnings</p><div class='big-num warn-color'>" + warnings + "</div></div>\n" +
        "          <!-- Donut -->\n" +
        "          <div class='kpi-card donut-card'>\n" +
        "            <p class='kpi-label'>Severity Split</p>\n" +
        "            <svg class='donut' viewBox='0 0 100 100'>\n" +
        "              <g transform='rotate(-90 50 50)'>\n" +
        "                <circle cx='50' cy='50' r='40' fill='none' stroke='#ef4444' stroke-width='20'\n" +
        "                        stroke-dasharray='" + fmt(errSeg)  + " " + fmt(dc) + "'\n" +
        "                        stroke-dashoffset='" + fmt(-errOff)  + "'/>\n" +
        "                <circle cx='50' cy='50' r='40' fill='none' stroke='#f59e0b' stroke-width='20'\n" +
        "                        stroke-dasharray='" + fmt(warnSeg) + " " + fmt(dc) + "'\n" +
        "                        stroke-dashoffset='" + fmt(-warnOff) + "'/>\n" +
        "                <circle cx='50' cy='50' r='40' fill='none' stroke='#38bdf8' stroke-width='20'\n" +
        "                        stroke-dasharray='" + fmt(infoSeg) + " " + fmt(dc) + "'\n" +
        "                        stroke-dashoffset='" + fmt(-infoOff) + "'/>\n" +
        "              </g>\n" +
        "              <circle cx='50' cy='50' r='30' fill='#0f172a'/>\n" +
        "              <text x='50' y='54' class='donut-label'>" + totalIssues + "</text>\n" +
        "            </svg>\n" +
        "            <div class='legend'>\n" +
        "              <span class='leg err-color'>● ERROR " + errors + "</span>\n" +
        "              <span class='leg warn-color'>● WARN " + warnings + "</span>\n" +
        "              <span class='leg info-color'>● INFO " + info + "</span>\n" +
        "            </div>\n" +
        "          </div>\n" +
        "        </div>\n" +
        "      </section>\n" +
        "\n" +
        "      <!-- ── Section: Rule Breakdown ── -->\n" +
        "      <section id='breakdown'>\n" +
        "        <h2 class='section-title'>Rule Breakdown</h2>\n" +
        "        <div class='table-wrap'>\n" +
        "          <table>\n" +
        "            <thead><tr><th>Anti-Pattern</th><th>Frequency</th><th>#</th></tr></thead>\n" +
        "            <tbody>" + ruleRows + "</tbody>\n" +
        "          </table>\n" +
        "        </div>\n" +
        "      </section>\n" +
        "\n" +
        "      <!-- ── Section: Issues ── -->\n" +
        "      <section id='issues'>\n" +
        "        <div class='issues-header'>\n" +
        "          <h2 class='section-title' style='margin:0'>Issues Detail</h2>\n" +
        "          <div class='filters'>\n" +
        "            <button class='filter-btn active' onclick='filter(\"ALL\")'>All</button>\n" +
        "            <button class='filter-btn err-btn'  onclick='filter(\"ERROR\")'>ERROR</button>\n" +
        "            <button class='filter-btn warn-btn' onclick='filter(\"WARN\")'>WARN</button>\n" +
        "            <button class='filter-btn info-btn' onclick='filter(\"INFO\")'>INFO</button>\n" +
        "          </div>\n" +
        "        </div>\n" +
        "        <div class='table-wrap'>\n" +
        "          <table id='issueTable'>\n" +
        "            <thead><tr><th style='width:90px'>Severity</th><th>Anti-Pattern</th><th style='width:170px'>Location</th><th>SQL Preview</th><th>Suggested Fix</th></tr></thead>\n" +
        "            <tbody>" + issueRows + "</tbody>\n" +
        "          </table>\n" +
        "          <p id='emptyMsg' class='empty-msg hidden'>No issues match this filter.</p>\n" +
        "        </div>\n" +
        "      </section>\n" +
        "    </main>\n" +
        "  </div>\n" +
        "\n" +
        "  <script>\n" + jsBlock() + "\n  </script>\n" +
        "</body>\n</html>\n";
    }

    // ─── CSS ────────────────────────────────────────────────────────────────

    private String cssBlock(String scoreColor, double circumference, double offset) {
        return
        "    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n" +
        "    :root {\n" +
        "      --bg:      #0b1120;\n" +
        "      --surface: #0f172a;\n" +
        "      --card:    #1e293b;\n" +
        "      --border:  #334155;\n" +
        "      --text:    #f1f5f9;\n" +
        "      --muted:   #94a3b8;\n" +
        "      --accent:  #38bdf8;\n" +
        "      --err:     #ef4444;\n" +
        "      --warn:    #f59e0b;\n" +
        "      --info:    #38bdf8;\n" +
        "      --score:   " + scoreColor + ";\n" +
        "    }\n" +
        "    body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); line-height: 1.6; }\n" +
        "    a { text-decoration: none; color: inherit; }\n" +
        "\n" +
        "    /* Layout */\n" +
        "    .layout { display: flex; min-height: 100vh; }\n" +
        "\n" +
        "    /* Sidebar */\n" +
        "    .sidebar {\n" +
        "      width: 220px; min-height: 100vh; background: var(--surface);\n" +
        "      border-right: 1px solid var(--border);\n" +
        "      display: flex; flex-direction: column; padding: 20px 0;\n" +
        "      position: sticky; top: 0; height: 100vh; overflow-y: auto;\n" +
        "    }\n" +
        "    .brand { display: flex; align-items: center; gap: 8px; padding: 0 20px 24px; border-bottom: 1px solid var(--border); }\n" +
        "    .brand-icon { font-size: 1.6em; }\n" +
        "    .brand-text { font-size: 1.2em; letter-spacing: -0.5px; }\n" +
        "    .nav { display: flex; flex-direction: column; gap: 4px; padding: 16px 12px; flex: 1; }\n" +
        "    .nav-item {\n" +
        "      padding: 10px 12px; border-radius: 8px; font-size: 0.9em;\n" +
        "      color: var(--muted); font-weight: 500; transition: all 0.2s;\n" +
        "    }\n" +
        "    .nav-item:hover, .nav-item.active { background: var(--card); color: var(--text); }\n" +
        "    .ts { padding: 16px 20px; font-size: 0.75em; color: var(--muted); border-top: 1px solid var(--border); }\n" +
        "    .ts span { color: var(--text); display: block; margin-top: 2px; }\n" +
        "\n" +
        "    /* Main */\n" +
        "    .main { flex: 1; padding: 36px 40px; overflow-y: auto; max-width: calc(100vw - 220px); }\n" +
        "\n" +
        "    /* Header */\n" +
        "    .page-header {\n" +
        "      display: flex; justify-content: space-between; align-items: flex-start;\n" +
        "      margin-bottom: 36px; padding-bottom: 24px; border-bottom: 1px solid var(--border);\n" +
        "    }\n" +
        "    .page-header h1 { font-size: 1.8em; font-weight: 800; letter-spacing: -0.5px; }\n" +
        "    .subtitle { color: var(--muted); margin-top: 4px; font-size: 0.95em; }\n" +
        "    .header-badge {\n" +
        "      background: var(--score); color: #000; font-weight: 700;\n" +
        "      padding: 6px 18px; border-radius: 999px; font-size: 0.9em; white-space: nowrap;\n" +
        "    }\n" +
        "\n" +
        "    /* KPI Grid */\n" +
        "    .kpi-grid {\n" +
        "      display: grid;\n" +
        "      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));\n" +
        "      gap: 16px; margin-bottom: 40px;\n" +
        "    }\n" +
        "    .kpi-card {\n" +
        "      background: var(--card); border: 1px solid var(--border);\n" +
        "      border-radius: 12px; padding: 20px 16px;\n" +
        "      display: flex; flex-direction: column; align-items: center;\n" +
        "      animation: fadeUp 0.4s ease both;\n" +
        "    }\n" +
        "    .kpi-label { font-size: 0.78em; color: var(--muted); text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 10px; text-align:center; }\n" +
        "    .score-card { grid-column: span 1; }\n" +
        "    .donut-card { grid-column: span 2; }\n" +
        "\n" +
        "    /* Score ring */\n" +
        "    .ring { width: 96px; height: 96px; }\n" +
        "    .ring-bg   { fill: none; stroke: var(--border); stroke-width: 10; }\n" +
        "    .ring-fill { fill: none; stroke-width: 10; stroke-linecap: round; transform: rotate(-90deg); transform-origin: center;\n" +
        "                 animation: ringFill 1.2s ease-out both; }\n" +
        "    .ring-num  { fill: var(--text); font-size: 22px; font-weight: 800; text-anchor: middle; dominant-baseline: middle; }\n" +
        "    .ring-sub  { fill: var(--muted); font-size: 9px; text-anchor: middle; }\n" +
        "\n" +
        "    /* Donut */\n" +
        "    .donut { width: 90px; height: 90px; }\n" +
        "    .donut-label { fill: var(--text); font-size: 14px; font-weight: 700; text-anchor: middle; dominant-baseline: middle; }\n" +
        "    .legend { display: flex; flex-direction: column; gap: 4px; margin-top: 10px; font-size: 0.78em; }\n" +
        "    .leg { color: var(--muted); }\n" +
        "\n" +
        "    /* Big numbers */\n" +
        "    .big-num { font-size: 2.8em; font-weight: 800; color: var(--accent); }\n" +
        "    .issues-color { color: #a78bfa; }\n" +
        "    .err-color    { color: var(--err); }\n" +
        "    .warn-color   { color: var(--warn); }\n" +
        "    .info-color   { color: var(--info); }\n" +
        "\n" +
        "    /* Section titles */\n" +
        "    .section-title { font-size: 1.2em; font-weight: 700; margin-bottom: 16px; color: var(--text); }\n" +
        "\n" +
        "    /* Tables */\n" +
        "    .table-wrap { overflow-x: auto; border-radius: 12px; border: 1px solid var(--border); margin-bottom: 40px; }\n" +
        "    table { width: 100%; border-collapse: collapse; }\n" +
        "    th { background: #1a2740; padding: 12px 16px; text-align: left; font-size: 0.8em;\n" +
        "         text-transform: uppercase; letter-spacing: 0.06em; color: var(--muted); font-weight: 600; }\n" +
        "    td { padding: 12px 16px; border-top: 1px solid var(--border); vertical-align: top; font-size: 0.88em; }\n" +
        "    tr:hover td { background: rgba(255,255,255,0.03); }\n" +
        "\n" +
        "    /* Badge */\n" +
        "    .badge { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: 0.75em; font-weight: 700; }\n" +
        "    .sev-ERROR { background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.4); }\n" +
        "    .sev-WARN  { background: rgba(245,158,11,0.15); color: #f59e0b; border: 1px solid rgba(245,158,11,0.4); }\n" +
        "    .sev-INFO  { background: rgba(56,189,248,0.15); color: #38bdf8; border: 1px solid rgba(56,189,248,0.4); }\n" +
        "\n" +
        "    /* Location cell */\n" +
        "    .loc { font-family: monospace; white-space: nowrap; }\n" +
        "    .file-name { color: var(--accent); }\n" +
        "    .line-num  { color: var(--muted); }\n" +
        "    .fix { color: #86efac; font-size: 0.85em; }\n" +
        "    .num { text-align: right; font-weight: 600; }\n" +
        "    code.sql-snippet { font-family: 'Consolas','Courier New',monospace; font-size: 0.78em; background: #0d1b2e; padding: 3px 7px; border-radius: 4px; color: #93c5fd; white-space: nowrap; max-width: 300px; overflow: hidden; text-overflow: ellipsis; display: block; }\n" +
        "\n" +
        "    /* Horizontal bar */\n" +
        "    .bar-wrap  { background: var(--border); border-radius: 4px; height: 8px; overflow: hidden; width: 140px; }\n" +
        "    .bar-fill  { height: 100%; background: linear-gradient(90deg, #38bdf8, #818cf8); border-radius: 4px;\n" +
        "                 animation: barGrow 0.8s ease both; }\n" +
        "\n" +
        "    /* Filters */\n" +
        "    .issues-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }\n" +
        "    .filters { display: flex; gap: 8px; }\n" +
        "    .filter-btn {\n" +
        "      padding: 6px 14px; border-radius: 999px; border: 1px solid var(--border);\n" +
        "      background: transparent; color: var(--muted); font-size: 0.8em; cursor: pointer;\n" +
        "      font-family: 'Inter', sans-serif; font-weight: 600; transition: all 0.2s;\n" +
        "    }\n" +
        "    .filter-btn.active, .filter-btn:hover { background: var(--card); color: var(--text); border-color: var(--accent); }\n" +
        "    .err-btn.active  { border-color: var(--err);  color: var(--err); }\n" +
        "    .warn-btn.active { border-color: var(--warn); color: var(--warn); }\n" +
        "    .info-btn.active { border-color: var(--info); color: var(--info); }\n" +
        "\n" +
        "    .empty-msg { text-align: center; padding: 32px; color: var(--muted); }\n" +
        "    .hidden { display: none; }\n" +
        "\n" +
        "    /* Animations */\n" +
        "    @keyframes fadeUp   { from { opacity:0; transform: translateY(16px); } to { opacity:1; transform: translateY(0); } }\n" +
        "    @keyframes ringFill { from { stroke-dashoffset: " + fmt(circumference) + "; } }\n" +
        "    @keyframes barGrow  { from { width: 0; } }\n" +
        "\n" +
        "    /* Responsive */\n" +
        "    @media (max-width: 768px) {\n" +
        "      .layout { flex-direction: column; }\n" +
        "      .sidebar { width: 100%; min-height: auto; flex-direction: row; flex-wrap: wrap; height: auto; position: static; }\n" +
        "      .main { max-width: 100%; padding: 20px; }\n" +
        "      .donut-card { grid-column: span 1; }\n" +
        "    }\n";
    }

    // ─── JS ─────────────────────────────────────────────────────────────────

    private String jsBlock() {
        return
        "    function filter(sev) {\n" +
        "      document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));\n" +
        "      const btn = [...document.querySelectorAll('.filter-btn')].find(b => b.textContent.trim() === sev || (sev==='ALL' && b.textContent.trim()==='All'));\n" +
        "      if (btn) btn.classList.add('active');\n" +
        "      let visible = 0;\n" +
        "      document.querySelectorAll('.issue-row').forEach(row => {\n" +
        "        const show = sev === 'ALL' || row.dataset.sev === sev;\n" +
        "        row.style.display = show ? '' : 'none';\n" +
        "        if (show) visible++;\n" +
        "      });\n" +
        "      document.getElementById('emptyMsg').classList.toggle('hidden', visible > 0);\n" +
        "    }\n" +
        "    // Smooth scroll nav\n" +
        "    document.querySelectorAll('.nav-item').forEach(a => {\n" +
        "      a.addEventListener('click', e => {\n" +
        "        e.preventDefault();\n" +
        "        document.querySelectorAll('.nav-item').forEach(x => x.classList.remove('active'));\n" +
        "        a.classList.add('active');\n" +
        "        const target = document.querySelector(a.getAttribute('href'));\n" +
        "        if (target) target.scrollIntoView({ behavior: 'smooth' });\n" +
        "      });\n" +
        "    });\n";
    }

    // ─── JSON export ─────────────────────────────────────────────────────────

    private void writeJSON(AnalysisResult result, File buildDir) {
        try {
            File reportDir = new File(buildDir, "reports/sql-analyzer");
            if (!reportDir.exists()) reportDir.mkdirs();
            File jsonFile = new File(reportDir, "report.json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            List<Map<String, Object>> issueList = result.getIssues().stream().map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("severity",   i.getSeverity().name());
                m.put("message",    i.getMessage());
                m.put("file",       i.getFile().getAbsolutePath());
                m.put("line",       i.getLine());
                m.put("suggestion", i.getSuggestion());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("score",         result.getScore());
            report.put("rating",        rating(result.getScore()));
            report.put("totalQueries",  result.getTotalQueries());
            report.put("totalIssues",   result.getIssues().size());
            report.put("errors",        count(result, Severity.ERROR));
            report.put("warnings",      count(result, Severity.WARN));
            report.put("issues",        issueList);

            Files.write(jsonFile.toPath(), gson.toJson(report).getBytes(StandardCharsets.UTF_8));
            System.out.println("======> JSON Report:    " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write JSON report: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int count(AnalysisResult r, Severity s) {
        return (int) r.getIssues().stream().filter(i -> i.getSeverity() == s).count();
    }

    private String rating(int score) {
        if (score >= 90) return "A — Excellent";
        if (score >= 75) return "B — Good";
        if (score >= 50) return "C — Needs Work";
        return "F — Critical";
    }

    /** Escape HTML special chars to prevent XSS in the report. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String fmt(double d) {
        return String.format("%.2f", d);
    }
}
