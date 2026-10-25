package org.example.queryoptimizer.knowledge.Ingestion;


import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns plan-log.jsonl records into short, searchable text descriptions and stores them
 * in the vector store with metadata sourceType = "PLAN_LOG", so they stay separate from
 * documentation chunks (sourceType = "DOCUMENTATION").
 *
 * This class only reads a file path, so it does not depend on the evaluation package.
 */
@Service
public class PlanLogIngestionService {

    public static final String SOURCE_TYPE = "PLAN_LOG";
    private static final int BATCH_SIZE = 50;
    private static final Pattern GROUP = Pattern.compile("group ([A-Z]\\d+)");

    private final VectorStore vectorStore;
    private final ObjectMapper mapper;

    public PlanLogIngestionService(VectorStore vectorStore, ObjectMapper mapper) {
        this.vectorStore = vectorStore;
        this.mapper = mapper;
    }

    public record Result(String file, String label, int read, int ingested, int excluded, int unreadable) {}

    /**
     * @param file     path of a JSONL log written by the plan log generator
     * @param label    run label used in the chunk IDs; defaults to the file name
     * @param excludes query IDs to skip (use this for your held-out test queries)
     */
    public Result ingest(String file, String label, Set<String> excludes) throws IOException {
        Path path = Path.of(file);
        String runLabel = (label == null || label.isBlank()) ? path.getFileName().toString() : label;
        Set<String> skip = excludes.stream().map(s -> s.strip().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<Document> batch = new ArrayList<>();
        int read = 0, ingested = 0, excluded = 0, unreadable = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                read++;
                Map<String, Object> rec;
                try {
                    rec = parse(line);
                } catch (RuntimeException e) {
                    unreadable++;
                    continue;
                }
                String id = str(rec, "id");
                if (skip.contains(id.toUpperCase(Locale.ROOT))) {
                    excluded++;
                    continue;
                }
                batch.add(toDocument(rec, runLabel));
                ingested++;
                if (batch.size() >= BATCH_SIZE) {
                    vectorStore.add(batch);
                    batch = new ArrayList<>();
                }
            }
        }
        if (!batch.isEmpty()) {
            vectorStore.add(batch);
        }
        return new Result(file, runLabel, read, ingested, excluded, unreadable);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String line) {
        return (Map<String, Object>) mapper.readValue(line, Map.class);
    }

    // ---------------------------------------------------------------- document building

    Document toDocument(Map<String, Object> rec, String runLabel) {
        String id = str(rec, "id");
        String mode = str(rec, "mode");

        Map<String, Object> meta = new HashMap<>();
        meta.put("sourceType", SOURCE_TYPE);
        meta.put("benchmark", "tpch");
        meta.put("queryId", id);
        meta.put("category", str(rec, "category"));
        String note = str(rec, "note");
        Matcher gm = GROUP.matcher(note);
        if (gm.find()) {
            meta.put("group", gm.group(1));   // equivalent formulations share a group label
        }
        meta.put("note", cut(note, 300));
        meta.put("mode", mode);
        meta.put("run", runLabel);
        if (rec.get("totalCost") instanceof Number n) meta.put("totalCost", n.doubleValue());
        if (rec.get("executionMs") instanceof Number n && "analyze".equals(mode)) {
            meta.put("executionMs", n.doubleValue());
        }
        meta.put("failed", !str(rec, "error").isBlank());
        meta.put("sql", cut(str(rec, "sql"), 1500));

        String docId = UUID.nameUUIDFromBytes(
                ("plan-log:" + runLabel + ":" + id + ":" + mode).getBytes(StandardCharsets.UTF_8)).toString();

        return new Document(docId, describe(rec), meta);
    }

    /** Builds the text that gets embedded. Describes the plan situation, not the raw JSON. */
    static String describe(Map<String, Object> rec) {
        StringBuilder t = new StringBuilder();
        t.append("Baseline plan observation for TPC-H query ").append(str(rec, "id"))
                .append(" (category: ").append(str(rec, "category")).append("). ")
                .append(str(rec, "note")).append(".\n");

        String err = str(rec, "error");
        if (!err.isBlank()) {
            if (err.toLowerCase(Locale.ROOT).contains("timeout")) {
                t.append("The baseline query hit the statement timeout and did not finish, so this plan is too slow as written.\n");
            } else {
                t.append("The baseline query failed: ").append(cut(err, 200)).append("\n");
            }
            return t.toString();
        }

        Map<String, Object> plan = asMap(rec.get("plan"));
        t.append("Plan shape: ").append(cut(shape(plan, 0), 700)).append("\n");

        List<Map<String, Object>> nodes = new ArrayList<>();
        walk(plan, nodes);

        List<String> scans = new ArrayList<>();
        List<String> indexes = new ArrayList<>();
        Map<String, Integer> joins = new java.util.LinkedHashMap<>();
        List<String> sorts = new ArrayList<>();
        List<String[]> estimateErrors = new ArrayList<>();

        for (Map<String, Object> n : nodes) {
            String type = str(n, "Node Type");
            String rel = str(n, "Relation Name");
            if (type.equals("Seq Scan") || type.equals("Parallel Seq Scan")) {
                String s = "Seq Scan on " + rel + " (estimated " + fmt(num(n, "Plan Rows")) + " rows";
                double removed = num(n, "Rows Removed by Filter");
                if (!Double.isNaN(removed)) s += ", filter removed " + fmt(removed);
                scans.add(s + ")");
            }
            if (type.contains("Index") || type.contains("Bitmap")) {
                String idx = str(n, "Index Name");
                indexes.add(type + (idx.isBlank() ? "" : " using " + idx) + (rel.isBlank() ? "" : " on " + rel));
            }
            if (type.endsWith("Join") || type.equals("Nested Loop")) {
                joins.merge(type, 1, Integer::sum);
            }
            if (type.equals("Sort")) {
                String method = str(n, "Sort Method");
                sorts.add(method.isBlank() ? "Sort" : "Sort (" + method + ")");
                if (method.toLowerCase(Locale.ROOT).contains("external")) {
                    sorts.add("the sort spilled to disk");
                }
            }
            double est = num(n, "Plan Rows");
            double act = num(n, "Actual Rows");
            if (!Double.isNaN(est) && !Double.isNaN(act)) {
                double hi = Math.max(est, act);
                double lo = Math.max(Math.min(est, act), 1.0);
                double ratio = hi / lo;
                if (ratio >= 10.0) {
                    estimateErrors.add(new String[]{String.valueOf(ratio),
                            type + (rel.isBlank() ? "" : " on " + rel)
                                    + ": estimated " + fmt(est) + " rows, actual " + fmt(act)});
                }
            }
        }

        if (!scans.isEmpty()) t.append("Sequential scans: ").append(String.join("; ", scans)).append(".\n");
        if (!indexes.isEmpty()) t.append("Index access: ").append(String.join("; ", indexes)).append(".\n");
        if (!joins.isEmpty()) {
            t.append("Joins: ").append(joins.entrySet().stream()
                    .map(e -> e.getKey() + " x" + e.getValue()).collect(Collectors.joining(", "))).append(".\n");
        }
        if (!sorts.isEmpty()) t.append("Sorting: ").append(String.join("; ", sorts)).append(".\n");

        if (!estimateErrors.isEmpty()) {
            estimateErrors.sort((a, b) -> Double.compare(Double.parseDouble(b[0]), Double.parseDouble(a[0])));
            t.append("Row estimate errors of 10x or more: ")
                    .append(estimateErrors.stream().limit(3).map(a -> a[1]).collect(Collectors.joining("; ")))
                    .append(".\n");
        }

        Object cost = rec.get("totalCost");
        if (cost instanceof Number c) t.append("Estimated total cost: ").append(fmt(c.doubleValue())).append(".\n");
        if ("analyze".equals(str(rec, "mode")) && rec.get("executionMs") instanceof Number ms) {
            t.append("Measured execution time: ").append(fmt(ms.doubleValue())).append(" ms.\n");
        }
        return t.toString();
    }

    private static String shape(Map<String, Object> node, int depth) {
        if (node.isEmpty()) return "";
        String label = str(node, "Node Type");
        String rel = str(node, "Relation Name");
        if (!rel.isBlank()) label += " on " + rel;
        List<Object> children = asList(node.get("Plans"));
        if (children.isEmpty()) return label;
        if (depth >= 8) return label + "(...)";
        List<String> parts = new ArrayList<>();
        for (Object c : children) parts.add(shape(asMap(c), depth + 1));
        return label + "(" + String.join(", ", parts) + ")";
    }

    private static void walk(Map<String, Object> node, List<Map<String, Object>> out) {
        if (node.isEmpty()) return;
        out.add(node);
        for (Object c : asList(node.get("Plans"))) walk(asMap(c), out);
    }

    // ---------------------------------------------------------------- small helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private static double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    private static String fmt(double d) {
        if (Double.isNaN(d)) return "?";
        if (Math.abs(d) >= 1000) return String.format(Locale.ROOT, "%,.0f", d);
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
