package org.example.queryoptimizer.Common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facts read from a PostgreSQL EXPLAIN (FORMAT JSON) plan tree (the value of the "Plan" key).
 * Used both to describe plans for the vector store and to decide whether a plan looks suspicious,
 * so the text of stored observations and the text of a search query have the same shape.
 */
public final class PlanFacts {

    public record SeqScan(String relation, double estimatedRows, double removedByFilter) {}

    public record ScanFilter(String relation, String filter) {}

    public record EstimateError(String node, double estimated, double actual) {
        public double ratio() {
            double hi = Math.max(estimated, actual);
            double lo = Math.max(Math.min(estimated, actual), 1.0);
            return hi / lo;
        }
    }

    private String shape = "";
    private double totalCost = Double.NaN;
    private final List<SeqScan> seqScans = new ArrayList<>();
    private final List<String> indexAccess = new ArrayList<>();
    private final Map<String, Integer> joins = new LinkedHashMap<>();
    private final List<String> sorts = new ArrayList<>();
    private boolean sortSpilled;
    private final List<EstimateError> estimateErrors = new ArrayList<>();
    private final List<ScanFilter> scanFilters = new ArrayList<>();
    private boolean correlatedSubPlan;
    private int largeNestedLoops;
    private int scanNodes;
    private final Set<String> relations = new LinkedHashSet<>();

    private PlanFacts() {}

    public static PlanFacts of(Map<String, Object> plan) {
        PlanFacts f = new PlanFacts();
        f.shape = shape(plan, 0);
        f.totalCost = num(plan, "Total Cost");
        f.visit(plan);
        f.estimateErrors.sort(Comparator.comparingDouble(EstimateError::ratio).reversed());
        return f;
    }

    // ------------------------------------------------------------------ accessors

    public double totalCost() { return totalCost; }
    public List<SeqScan> seqScans() { return seqScans; }
    public List<ScanFilter> scanFilters() { return scanFilters; }
    public List<EstimateError> estimateErrors() { return estimateErrors; }
    public boolean sortSpilled() { return sortSpilled; }
    public boolean correlatedSubPlan() { return correlatedSubPlan; }
    public int largeNestedLoops() { return largeNestedLoops; }
    public int scanNodes() { return scanNodes; }
    public Set<String> relations() { return relations; }
    public String shape() { return shape; }

    // ------------------------------------------------------------------ description

    /** The text that is embedded for stored observations and used as the search query. */
    public String describe(Double executionMs) {
        StringBuilder t = new StringBuilder();
        t.append("Plan shape: ").append(cut(shape, 700)).append("\n");

        if (!seqScans.isEmpty()) {
            t.append("Sequential scans: ").append(seqScans.stream().map(s ->
                    "Seq Scan on " + s.relation() + " (estimated " + fmt(s.estimatedRows()) + " rows"
                            + (Double.isNaN(s.removedByFilter()) ? "" : ", filter removed " + fmt(s.removedByFilter()))
                            + ")").collect(Collectors.joining("; "))).append(".\n");
        }
        if (!indexAccess.isEmpty()) {
            t.append("Index access: ").append(String.join("; ", indexAccess)).append(".\n");
        }
        if (!joins.isEmpty()) {
            t.append("Joins: ").append(joins.entrySet().stream()
                    .map(e -> e.getKey() + " x" + e.getValue()).collect(Collectors.joining(", "))).append(".\n");
        }
        if (!sorts.isEmpty()) {
            t.append("Sorting: ").append(String.join("; ", sorts)).append(".\n");
        }
        if (!estimateErrors.isEmpty()) {
            t.append("Row estimate errors of 10x or more: ").append(estimateErrors.stream().limit(3)
                    .map(e -> e.node() + ": estimated " + fmt(e.estimated()) + " rows, actual " + fmt(e.actual()))
                    .collect(Collectors.joining("; "))).append(".\n");
        }
        if (!Double.isNaN(totalCost)) {
            t.append("Estimated total cost: ").append(fmt(totalCost)).append(".\n");
        }
        if (executionMs != null) {
            t.append("Measured execution time: ").append(fmt(executionMs)).append(" ms.\n");
        }
        return t.toString();
    }

    // ------------------------------------------------------------------ tree walk

    private void visit(Map<String, Object> n) {
        if (n.isEmpty()) return;
        String type = str(n, "Node Type");
        String rel = str(n, "Relation Name");
        List<Object> kids = list(n.get("Plans"));

        if (!rel.isBlank()) {
            relations.add(rel);
            if (type.contains("Scan")) scanNodes++;
        }
        if (type.equals("Seq Scan") || type.equals("Parallel Seq Scan")) {
            seqScans.add(new SeqScan(rel, num(n, "Plan Rows"), num(n, "Rows Removed by Filter")));
        }
        if (type.contains("Index") || type.contains("Bitmap")) {
            String idx = str(n, "Index Name");
            indexAccess.add(type + (idx.isBlank() ? "" : " using " + idx) + (rel.isBlank() ? "" : " on " + rel));
        }
        if (type.endsWith("Join") || type.equals("Nested Loop")) {
            joins.merge(type, 1, Integer::sum);
        }
        if (type.equals("Sort") || type.equals("Incremental Sort")) {
            String method = str(n, "Sort Method");
            sorts.add(method.isBlank() ? type : type + " (" + method + ")");
            if (method.toLowerCase(Locale.ROOT).contains("external")) {
                sortSpilled = true;
                sorts.add("the sort spilled to disk");
            }
        }
        double est = num(n, "Plan Rows");
        double act = num(n, "Actual Rows");
        if (!Double.isNaN(est) && !Double.isNaN(act)) {
            EstimateError e = new EstimateError(type + (rel.isBlank() ? "" : " on " + rel), est, act);
            if (e.ratio() >= 10.0) estimateErrors.add(e);
        }
        String filter = str(n, "Filter");
        if (!filter.isBlank() && !rel.isBlank()) {
            scanFilters.add(new ScanFilter(rel, filter));
        }
        if ("SubPlan".equals(str(n, "Parent Relationship"))
                && !str(n, "Subplan Name").toLowerCase(Locale.ROOT).startsWith("hashed")) {
            correlatedSubPlan = true;
        }
        if (type.equals("Nested Loop") && kids.size() >= 2) {
            Map<String, Object> outer = asMap(kids.get(0));
            Map<String, Object> inner = asMap(kids.get(1));
            if (num(outer, "Plan Rows") >= 10_000 && seqLike(inner)) largeNestedLoops++;
        }
        for (Object c : kids) visit(asMap(c));
    }

    private static boolean seqLike(Map<String, Object> n) {
        String t = str(n, "Node Type");
        if (t.contains("Seq Scan")) return true;
        if (t.equals("Materialize")) {
            List<Object> k = list(n.get("Plans"));
            return !k.isEmpty() && str(asMap(k.get(0)), "Node Type").contains("Seq Scan");
        }
        return false;
    }

    private static String shape(Map<String, Object> node, int depth) {
        if (node.isEmpty()) return "";
        String label = str(node, "Node Type");
        String rel = str(node, "Relation Name");
        if (!rel.isBlank()) label += " on " + rel;
        List<Object> children = list(node.get("Plans"));
        if (children.isEmpty()) return label;
        if (depth >= 8) return label + "(...)";
        List<String> parts = new ArrayList<>();
        for (Object c : children) parts.add(shape(asMap(c), depth + 1));
        return label + "(" + String.join(", ", parts) + ")";
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
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

    public static String fmt(double d) {
        if (Double.isNaN(d)) return "?";
        if (Math.abs(d) >= 1000) return String.format(Locale.ROOT, "%,.0f", d);
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
