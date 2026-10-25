package org.example.queryoptimizer.Optimizer.Engine;


import org.example.queryoptimizer.Common.PlanFacts;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a baseline plan shows signs of a problem. No flags means the plan looks fine
 * and the original query is kept without calling the LLM. Each flag also names the documentation
 * topics that explain it, which is how the search is narrowed.
 */
@Component
public class PlanAssessor {

    private static final long LARGE_TABLE_ROWS = 100_000;
    private static final double SELECTIVE_FRACTION = 0.10;
    private static final List<String> FUNCTION_MARKERS = List.of(
            "date_part(", "date_trunc(", "upper(", "lower(", "substr(", "substring(", "to_char(", "extract(");
    private static final Pattern NOT_IN_SUBQUERY = Pattern.compile("not\\s+in\\s*\\(\\s*select");
    private static final Pattern DEEP_OFFSET = Pattern.compile("offset\\s+(\\d{4,})");

    public record Flag(String code, String description, List<String> topics) {}

    public List<Flag> assess(String sql, PlanFacts facts, Map<String, Long> tableRows) {
        List<Flag> flags = new ArrayList<>();

        for (PlanFacts.SeqScan s : facts.seqScans()) {
            Long rows = tableRows.get(s.relation());
            if (rows != null && rows >= LARGE_TABLE_ROWS && s.estimatedRows() / rows <= SELECTIVE_FRACTION) {
                flags.add(new Flag("SELECTIVE_SEQ_SCAN",
                        "Seq Scan on " + s.relation() + " reads a table of about " + PlanFacts.fmt(rows)
                                + " rows but keeps only about " + PlanFacts.fmt(s.estimatedRows())
                                + " rows, so an index on the filter column may help",
                        List.of("INDEXES", "INDEX_USAGE")));
            }
        }
        for (PlanFacts.ScanFilter f : facts.scanFilters()) {
            Long rows = tableRows.get(f.relation());
            String lower = f.filter().toLowerCase(Locale.ROOT);
            if (rows != null && rows >= LARGE_TABLE_ROWS && FUNCTION_MARKERS.stream().anyMatch(lower::contains)) {
                flags.add(new Flag("FUNCTION_ON_COLUMN",
                        "The filter on " + f.relation() + " wraps a column in a function, which prevents a plain index from being used: "
                                + f.filter(),
                        List.of("INDEXES", "INDEX_USAGE")));
                break;
            }
        }
        if (!facts.estimateErrors().isEmpty()) {
            PlanFacts.EstimateError e = facts.estimateErrors().get(0);
            flags.add(new Flag("ESTIMATE_ERROR",
                    "Row estimate is far off at " + e.node() + ": estimated " + PlanFacts.fmt(e.estimated())
                            + " rows, actual " + PlanFacts.fmt(e.actual()) + ", so planner statistics may be outdated",
                    List.of("STATISTICS", "QUERY_PLANNER")));
        }
        if (facts.sortSpilled()) {
            flags.add(new Flag("SORT_SPILL", "A sort spilled to disk, so work_mem may be too small for this query",
                    List.of("QUERY_PLANNER", "EXECUTION_PLANS")));
        }
        if (facts.correlatedSubPlan()) {
            flags.add(new Flag("CORRELATED_SUBPLAN",
                    "The plan contains a correlated subquery that runs once per outer row",
                    List.of("SUBQUERIES", "JOINS")));
        }
        if (facts.largeNestedLoops() > 0) {
            flags.add(new Flag("NESTED_LOOP_LARGE_OUTER",
                    "A nested loop join has a large outer side and a sequential scan on the inner side",
                    List.of("JOINS", "EXECUTION_PLANS")));
        }
        if (facts.scanNodes() > 8) {
            flags.add(new Flag("MANY_RELATIONS",
                    "The query joins more than 8 relations, so the written join order may matter",
                    List.of("JOINS", "QUERY_PLANNER")));
        }

        String lowerSql = sql.toLowerCase(Locale.ROOT);
        if (NOT_IN_SUBQUERY.matcher(lowerSql).find()) {
            flags.add(new Flag("NOT_IN_SUBQUERY",
                    "NOT IN with a subquery can be slow and handles NULLs differently from NOT EXISTS",
                    List.of("SUBQUERIES", "JOINS")));
        }
        Matcher m = DEEP_OFFSET.matcher(lowerSql);
        if (m.find()) {
            flags.add(new Flag("DEEP_OFFSET",
                    "OFFSET " + m.group(1) + " reads and discards all skipped rows",
                    List.of("EXECUTION_PLANS", "INDEXES")));
        }
        return flags;
    }
}