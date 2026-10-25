package org.example.queryoptimizer.Evaluation;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Compares an original query with an optimized one on the target database:
 * median execution time (EXPLAIN ANALYZE), estimated cost, and result equivalence.
 * Evaluation only: EXPLAIN ANALYZE really executes the query.
 */
@Service
public class ExperimentService {

    private static final int WARMUP_RUNS = 1;

    private final JdbcTemplate target;
    private final ObjectMapper mapper;

    public ExperimentService(@Qualifier("targetJdbcTemplate") JdbcTemplate target, ObjectMapper mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    public record PlanTiming(double planningMs, double executionMs, double totalCost) {}

    public record Comparison(PlanTiming original,
                             PlanTiming optimized,
                             boolean resultsMatch,
                             double speedup) {}

    public Comparison compare(String originalSql, String optimizedSql, int runs) {
        String original = requireSingleSelect(originalSql);
        String optimized = requireSingleSelect(optimizedSql);
        int measured = Math.max(1, runs);

        PlanTiming originalTiming = measure(original, measured);
        PlanTiming optimizedTiming = measure(optimized, measured);
        boolean match = fingerprint(original).equals(fingerprint(optimized));

        double speedup = optimizedTiming.executionMs() > 0
                ? originalTiming.executionMs() / optimizedTiming.executionMs()
                : 0.0;

        return new Comparison(originalTiming, optimizedTiming, match, speedup);
    }

    /** Runs EXPLAIN ANALYZE several times (after warm-up) and returns the median timings. */
    private PlanTiming measure(String sql, int runs) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            explainAnalyze(sql);
        }
        List<PlanTiming> samples = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            samples.add(explainAnalyze(sql));
        }
        samples.sort((a, b) -> Double.compare(a.executionMs(), b.executionMs()));
        return samples.get(samples.size() / 2);
    }

    private PlanTiming explainAnalyze(String sql) {
        String json = target.queryForObject("EXPLAIN (ANALYZE, FORMAT JSON) " + sql, String.class);
        try {
            JsonNode root = mapper.readTree(json).get(0);
            return new PlanTiming(
                    root.path("Planning Time").asDouble(),
                    root.path("Execution Time").asDouble(),
                    root.path("Plan").path("Total Cost").asDouble());
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse EXPLAIN output", e);
        }
    }

    /** Order-insensitive row count + hash, computed inside PostgreSQL. */
    private String fingerprint(String sql) {
        Map<String, Object> row = target.queryForMap(
                "SELECT count(*) AS n, md5(coalesce(string_agg(t::text, '|' ORDER BY t::text), '')) AS h "
                        + "FROM (" + sql + ") t");
        return row.get("n") + ":" + row.get("h");
    }

    /** Guard: only a single SELECT/WITH statement is allowed. */
    private String requireSingleSelect(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be empty");
        }
        String s = sql.strip();
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).strip();
        }
        String lower = s.toLowerCase();
        if (s.contains(";") || !(lower.startsWith("select") || lower.startsWith("with"))) {
            throw new IllegalArgumentException("Only a single SELECT query is allowed");
        }
        return s;
    }
}
