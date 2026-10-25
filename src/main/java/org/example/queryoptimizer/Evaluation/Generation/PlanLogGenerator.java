package org.example.queryoptimizer.Evaluation.Generation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs EXPLAIN for every query in a queries file against the target database
 * and writes one JSON object per line (JSONL) to a log file.
 */
@Service
public class PlanLogGenerator {

    private final JdbcTemplate target;
    private final ObjectMapper mapper;
    private final ResourceLoader resourceLoader;
    private final String queriesFile;
    private final String logPath;

    public PlanLogGenerator(@Qualifier("targetJdbcTemplate") JdbcTemplate target,
                            ObjectMapper mapper,
                            ResourceLoader resourceLoader,
                            @Value("${app.evaluation.queries-file:classpath:TpchQueries3.sql}") String queriesFile,
                            @Value("${app.evaluation.log-path:plan-log3.jsonl}") String logPath) {
        this.target = target;
        this.mapper = mapper;
        this.resourceLoader = resourceLoader;
        this.queriesFile = queriesFile;
        this.logPath = logPath;
    }

    public record Entry(String id, String category, String note, String sql) {}

    public record Summary(int total, int succeeded, int failed, String file, boolean analyze) {}

    public Summary generate(boolean analyze) throws IOException {
        List<Entry> entries = loadQueries();
        String prefix = analyze
                ? "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "
                : "EXPLAIN (FORMAT JSON) ";

        int ok = 0;
        int failed = 0;

        try (BufferedWriter out = Files.newBufferedWriter(Path.of(logPath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (Entry e : entries) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("id", e.id());
                rec.put("category", e.category());
                rec.put("note", e.note());
                rec.put("mode", analyze ? "analyze" : "plan-only");
                rec.put("sql", e.sql());

                try {
                    String json = target.queryForObject(prefix + e.sql(), String.class);
                    JsonNode root = mapper.readTree(json).get(0);
                    rec.put("totalCost", root.path("Plan").path("Total Cost").asDouble());
                    rec.put("planRows", root.path("Plan").path("Plan Rows").asLong());
                    rec.put("planningMs", root.path("Planning Time").asDouble());
                    rec.put("executionMs", root.path("Execution Time").asDouble());
                    rec.put("plan", root.path("Plan"));
                    ok++;
                } catch (Exception ex) {
                    // Timeouts and errors are useful data too, so they are logged, not skipped.
                    rec.put("error", String.valueOf(ex.getMessage()));
                    failed++;
                }

                out.write(mapper.writeValueAsString(rec));
                out.newLine();
            }
        }
        return new Summary(entries.size(), ok, failed, Path.of(logPath).toAbsolutePath().toString(), analyze);
    }

    /** Reads "-- ID | category | note" headers followed by a statement ending in ";". */
    private List<Entry> loadQueries() throws IOException {
        List<Entry> entries = new ArrayList<>();
        String id = null, category = "", note = "";
        StringBuilder sql = new StringBuilder();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                resourceLoader.getResource(queriesFile).getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                String t = line.strip();
                if (t.startsWith("--")) {
                    if (t.contains("|")) {
                        String[] p = t.substring(2).split("\\|");
                        id = p[0].strip();
                        category = p.length > 1 ? p[1].strip() : "";
                        note = p.length > 2 ? p[2].strip() : "";
                    }
                    continue;
                }
                if (t.isEmpty() && sql.length() == 0) {
                    continue;
                }
                sql.append(line).append('\n');
                if (t.endsWith(";")) {
                    String statement = sql.toString().strip();
                    statement = statement.substring(0, statement.length() - 1).strip();
                    entries.add(new Entry(id == null ? "Q" + (entries.size() + 1) : id,
                            category, note, statement));
                    sql.setLength(0);
                    id = null;
                }
            }
        }
        return entries;
    }
}
