package org.example.queryoptimizer.Target.Tools;


import org.example.queryoptimizer.Target.Dto.Measurement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything the optimizer needs from the TARGET database (TPC-H now): plans, row estimates,
 * schema and index information, and optional execution checks.
 * It uses only the target datasource, never the knowledge store.
 */
@Component
public class TargetDatabaseTool {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public TargetDatabaseTool(@Qualifier("targetJdbcTemplate") JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }



    /** EXPLAIN (FORMAT JSON) without executing. Returns the root object, which holds the "Plan" key. */
    public Map<String, Object> explain(String sql) {
        return root(jdbc.queryForObject("EXPLAIN (FORMAT JSON) " + sql, String.class));
    }

    /** EXPLAIN ANALYZE: really runs the query, so the connection's statement timeout applies. */
    public Measurement analyze(String sql) {
        Map<String, Object> root = root(jdbc.queryForObject("EXPLAIN (ANALYZE, FORMAT JSON) " + sql, String.class));
        Map<String, Object> plan = asMap(root.get("Plan"));
        return new Measurement(number(root.get("Execution Time")), number(plan.get("Total Cost")));
    }

    /** Order-insensitive row count and hash computed inside PostgreSQL, for result comparison. */
    public String fingerprint(String sql) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT count(*) AS n, md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) AS h FROM ("
                        + sql + ") t");
        return row.get("n") + ":" + row.get("h");
    }

    /** Estimated row counts from pg_class. Tables that were never analyzed are left out. */
    public Map<String, Long> rowEstimates(Collection<String> relations) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String r : relations) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT reltuples::bigint AS n FROM pg_class WHERE relname = ? AND relkind = 'r'", r);
            if (!rows.isEmpty() && rows.get(0).get("n") instanceof Number n && n.longValue() >= 0) {
                out.put(r, n.longValue());
            }
        }
        return out;
    }

    /** Columns and index definitions of the given tables, as text for the LLM prompt. */
    public String schemaContext(Collection<String> relations, Map<String, Long> rowEstimates) {
        StringBuilder sb = new StringBuilder();
        for (String r : relations) {
            sb.append("Table ").append(r);
            Long rows = rowEstimates.get(r);
            if (rows != null) sb.append(" (about ").append(String.format(Locale.ROOT, "%,d", rows)).append(" rows)");
            sb.append(": ");
            List<Map<String, Object>> cols = jdbc.queryForList(
                    "SELECT column_name, data_type FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position", r);
            sb.append(cols.stream().map(c -> c.get("column_name") + " " + c.get("data_type"))
                    .reduce((a, b) -> a + ", " + b).orElse("(no columns found)")).append("\n");
            List<Map<String, Object>> idx = jdbc.queryForList(
                    "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = ?", r);
            if (idx.isEmpty()) {
                sb.append("  Indexes: none\n");
            } else {
                for (Map<String, Object> i : idx) sb.append("  Index: ").append(i.get("indexdef")).append("\n");
            }
        }
        return sb.toString();
    }

    /** Only a single SELECT or WITH statement is accepted. Returns the statement without a trailing ";". */
    public static String requireSingleSelect(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be empty");
        }
        String s = sql.strip();
        if (s.endsWith(";")) s = s.substring(0, s.length() - 1).strip();
        String lower = s.toLowerCase(Locale.ROOT);
        if (s.contains(";") || !(lower.startsWith("select") || lower.startsWith("with"))) {
            throw new IllegalArgumentException("Only a single SELECT query is allowed");
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> root(String json) {
        List<Object> arr = (List<Object>) mapper.readValue(json, List.class);
        return (Map<String, Object>) arr.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    private static double number(Object o) {
        return o instanceof Number n ? n.doubleValue() : Double.NaN;
    }
}
