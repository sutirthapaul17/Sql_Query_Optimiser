package org.example.queryoptimizer.Optimizer.Engine;


import org.example.queryoptimizer.Common.PlanFacts;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Dto.Proposal;
import org.example.queryoptimizer.Target.Service.QueryResultService;
import org.example.queryoptimizer.knowledge.KnowledgeRetriever.Chunk;
import org.example.queryoptimizer.knowledge.Tools.KnowledgeSearchTool;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationOutcome.Decision;
import org.example.queryoptimizer.Optimizer.Engine.PlanAssessor.Flag;
import org.example.queryoptimizer.Target.Tools.TargetDatabaseTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The optimization flow:
 * 1. EXPLAIN the query on the target database (the baseline plan).
 * 2. Check the plan for problems. No problems: keep the original, no LLM call.
 * 3. Retrieve evidence from the knowledge store (documentation by topic, similar past plans).
 * 4. Ask the LLM for a decision and a candidate.
 * 5. Validate the candidate on the target database. Anything that fails validation is discarded,
 *    and the original query is returned.
 */
@Service
public class OptimizerEngine {

    private final QueryResultService queryResultService;
    private static final int MAX_CHUNK_CHARS = 1200;
    private static final int MAX_DOC_CHUNKS = 6;

    private final TargetDatabaseTool target;
    private final PlanAssessor assessor;
    private final KnowledgeSearchTool search;
    private final CandidateGenerator generator;

    private final double minImprovement;
    private final boolean verifyExecution;
    private final boolean alwaysConsultLlm;
    private final int maxAttempts;
    private final int docsPerFlag;
    private final int plansTopK;

    public OptimizerEngine(TargetDatabaseTool target, PlanAssessor assessor, KnowledgeSearchTool search,
                           CandidateGenerator generator,
                           QueryResultService queryResultService,
                           @Value("${app.optimizer.min-improvement:0.05}") double minImprovement,
                           @Value("${app.optimizer.verify-execution:false}") boolean verifyExecution,
                           @Value("${app.optimizer.always-consult-llm:false}") boolean alwaysConsultLlm,
                           @Value("${app.optimizer.max-attempts:2}") int maxAttempts,
                           @Value("${app.optimizer.docs-per-flag:2}") int docsPerFlag,
                           @Value("${app.optimizer.plans-top-k:4}") int plansTopK) {
        this.target = target;
        this.assessor = assessor;
        this.search = search;
        this.generator = generator;
        this.minImprovement = minImprovement;
        this.verifyExecution = verifyExecution;
        this.alwaysConsultLlm = alwaysConsultLlm;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.docsPerFlag = docsPerFlag;
        this.plansTopK = plansTopK;
        this.queryResultService = queryResultService;
    }

    @SuppressWarnings("unchecked")
    public OptimizationOutcome optimize(String rawSql) {
        String sql;
        try {
            sql = TargetDatabaseTool.requireSingleSelect(rawSql);
        } catch (IllegalArgumentException e) {
            return OptimizationOutcome.failed(rawSql, e.getMessage());
        }

        // 1. baseline plan from the target database
        Map<String, Object> root;
        try {
            root = target.explain(sql);
        } catch (RuntimeException e) {
            return OptimizationOutcome.failed(sql, "The target database could not explain the query: " + rootMessage(e));
        }
        PlanFacts facts = PlanFacts.of(PlanFacts.asMap(root.get("Plan")));
        double baselineCost = facts.totalCost();
        Map<String, Long> rows = target.rowEstimates(facts.relations());

        // 2. does the plan show problems?
        List<Flag> flags = assessor.assess(sql, facts, rows);
        List<String> flagText = flags.stream().map(f -> f.code() + ": " + f.description()).toList();
        if (flags.isEmpty() && !alwaysConsultLlm) {
            return outcome(Decision.KEEP_ORIGINAL, sql, sql,
                    "The baseline plan shows no known problem (no selective scan of a large table, no large estimate "
                            + "errors, no spilled sort, no correlated subquery), so the original query is kept.",
                    flagText, List.of(), List.of(), baselineCost, null, null, null, null, List.of(), List.of());
        }

        // 3. evidence from the knowledge store
        List<String> warnings = new ArrayList<>();
        List<Chunk> docs = List.of();
        List<Chunk> plans = List.of();
        try {
            docs = gatherDocs(flags, facts);
            plans = search.similarPlans(facts.describe(null), plansTopK);
        } catch (RuntimeException e) {
            warnings.add("Knowledge search failed, continuing without evidence: " + rootMessage(e));
        }
        List<String> evidence = new ArrayList<>();
        docs.forEach(c -> evidence.add("doc: " + meta(c, "title") + " (" + meta(c, "topic") + ")"));
        plans.forEach(c -> evidence.add("plan: " + meta(c, "queryId") + " " + meta(c, "note")));

        String schema = target.schemaContext(facts.relations(), rows);
        String prompt = buildPrompt(sql, facts, flagText, schema, docs, plans);

        // 4 and 5. propose, validate, retry once with feedback
        String feedback = null;
        String lastCandidate = null;
        List<String> indexRecs = List.of();
        List<String> settingRecs = List.of();
        String lastReasoning = "";

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Proposal p;
            try {
                p = generator.propose(prompt, lastCandidate, feedback);
            } catch (RuntimeException e) {
                warnings.add("Attempt " + attempt + ": " + rootMessage(e));
                feedback = "Return only one valid JSON object in the requested format.";
                continue;
            }
            indexRecs = p.indexRecommendations();
            settingRecs = p.settingRecommendations();
            lastReasoning = p.reasoning();

            if (!p.optimize()) {
                return outcome(Decision.KEEP_ORIGINAL, sql, sql, p.reasoning(), flagText, indexRecs, settingRecs,
                        baselineCost, null, null, null, null, evidence, warnings);
            }

            String candidate;
            try {
                candidate = TargetDatabaseTool.requireSingleSelect(p.optimizedSql());
            } catch (IllegalArgumentException e) {
                lastCandidate = p.optimizedSql();
                feedback = e.getMessage();
                warnings.add("Attempt " + attempt + ": candidate rejected, " + feedback);
                continue;
            }
            lastCandidate = candidate;
            if (normalize(candidate).equals(normalize(sql))) {
                return outcome(Decision.KEEP_ORIGINAL, sql, sql,
                        "The model returned the same query, so the original is kept. " + p.reasoning(),
                        flagText, indexRecs, settingRecs, baselineCost, null, null, null, null, evidence, warnings);
            }

            double candidateCost;
            try {
                candidateCost = PlanFacts.of(PlanFacts.asMap(target.explain(candidate).get("Plan"))).totalCost();
            } catch (RuntimeException e) {
                feedback = "PostgreSQL rejected the candidate: " + rootMessage(e);
                warnings.add("Attempt " + attempt + ": " + feedback);
                continue;
            }
            if (!(candidateCost <= baselineCost * (1.0 - minImprovement))) {
                feedback = "Its estimated cost " + PlanFacts.fmt(candidateCost) + " is not at least "
                        + Math.round(minImprovement * 100) + "% lower than the baseline cost "
                        + PlanFacts.fmt(baselineCost) + ".";
                warnings.add("Attempt " + attempt + ": " + feedback);
                continue;
            }

            Double baselineMs = null;
            Double candidateMs = null;
            Boolean match = null;
            if (verifyExecution) {
                String problem = null;
                try {
                    candidateMs = target.analyze(candidate).executionMs();
                    try {
                        baselineMs = target.analyze(sql).executionMs();
                    } catch (RuntimeException e) {
                        warnings.add("The baseline query did not finish in time, so the comparison uses the candidate only.");
                    }
                    try {
                        String a = target.fingerprint(sql);
                        match = a.equals(target.fingerprint(candidate));
                    } catch (RuntimeException e) {
                        warnings.add("Results could not be compared: " + rootMessage(e));
                    }
                    if (Boolean.FALSE.equals(match)) {
                        problem = "It returns different rows than the original.";
                    } else if (baselineMs != null && !(candidateMs <= baselineMs * (1.0 - minImprovement))) {
                        problem = "It ran in " + PlanFacts.fmt(candidateMs) + " ms against " + PlanFacts.fmt(baselineMs)
                                + " ms for the original.";
                    }
                } catch (RuntimeException e) {
                    problem = "It failed or timed out when executed: " + rootMessage(e);
                }
                if (problem != null) {
                    feedback = problem;
                    warnings.add("Attempt " + attempt + ": " + problem);
                    continue;
                }
            }
            return outcome(Decision.OPTIMIZED, sql, candidate, p.reasoning(), flagText, indexRecs, settingRecs,
                    baselineCost, candidateCost, baselineMs, candidateMs, match, evidence, warnings);
        }

        warnings.add("No valid candidate after " + maxAttempts + " attempts.");
        return outcome(Decision.KEEP_ORIGINAL, sql, sql,
                "No candidate passed validation, so the original query is kept. " + lastReasoning,
                flagText, indexRecs, settingRecs, baselineCost, null, null, null, null, evidence, warnings);
    }

    // ------------------------------------------------------------------ evidence and prompt

    private List<Chunk> gatherDocs(List<Flag> flags, PlanFacts facts) {
        Map<String, Chunk> docs = new LinkedHashMap<>();
        if (flags.isEmpty()) {
            for (Chunk c : search.documentation(facts.shape(), List.of("EXECUTION_PLANS", "QUERY_PLANNER"), docsPerFlag)) {
                docs.putIfAbsent(c.content(), c);
            }
        }
        for (Flag f : flags) {
            for (Chunk c : search.documentation(f.description(), f.topics(), docsPerFlag)) {
                docs.putIfAbsent(c.content(), c);
            }
        }
        return docs.values().stream().limit(MAX_DOC_CHUNKS).toList();
    }

    private String buildPrompt(String sql, PlanFacts facts, List<String> flags, String schema,
                               List<Chunk> docs, List<Chunk> plans) {
        StringBuilder p = new StringBuilder();
        p.append("ORIGINAL QUERY:\n").append(sql).append("\n\n");
        p.append("BASELINE PLAN (summary of PostgreSQL EXPLAIN):\n").append(facts.describe(null)).append("\n");
        p.append("POTENTIAL ISSUES DETECTED:\n");
        if (flags.isEmpty()) p.append("(none detected by the rules)\n");
        flags.forEach(f -> p.append("- ").append(f).append("\n"));
        p.append("\nSCHEMA OF THE TABLES IN THE PLAN:\n").append(schema).append("\n");
        p.append("DOCUMENTATION EXCERPTS:\n");
        if (docs.isEmpty()) p.append("(none found)\n");
        int i = 1;
        for (Chunk c : docs) {
            p.append("[").append(i++).append("] (").append(meta(c, "topic")).append(", ").append(meta(c, "title"))
                    .append(")\n").append(cut(c.content())).append("\n\n");
        }
        p.append("SIMILAR PAST BASELINE OBSERVATIONS (they describe plans, not fixes):\n");
        if (plans.isEmpty()) p.append("(none found)\n");
        i = 1;
        for (Chunk c : plans) {
            p.append("[").append(i++).append("] ").append(cut(c.content())).append("\n");
        }
        p.append("\nDecide whether the baseline plan should be kept or whether a better query exists.");
        return p.toString();
    }

    // ------------------------------------------------------------------ small helpers

//    private static OptimizationOutcome outcome(Decision d, String original, String finalSql, String reasoning,
//                                               List<String> flags, List<String> idx, List<String> settings,
//                                               Double baselineCost, Double candidateCost, Double baselineMs,
//                                               Double candidateMs, Boolean match, List<String> evidence,
//                                               List<String> warnings) {
//        return new OptimizationOutcome(d, original, finalSql, reasoning, flags, idx, settings, baselineCost,
//                candidateCost, baselineMs, candidateMs, match, evidence, warnings,List.of(),
//                false  );
//    }

    private OptimizationOutcome outcome(
            Decision d,
            String original,
            String finalSql,
            String reasoning,
            List<String> flags,
            List<String> idx,
            List<String> settings,
            Double baselineCost,
            Double candidateCost,
            Double baselineMs,
            Double candidateMs,
            Boolean match,
            List<String> evidence,
            List<String> warnings) {

        List<String> responseWarnings = new ArrayList<>(warnings);
        List<Map<String, Object>> retrievedData = List.of();
        Boolean dataTruncated = false;

        try {
            QueryResultService.QueryResult result =
                    queryResultService.execute(finalSql);

            retrievedData = result.rows();
            dataTruncated = result.truncated();

            if (dataTruncated) {
                responseWarnings.add(
                        "Results were limited to the configured maximum row count."
                );
            }

        } catch (RuntimeException e) {
            responseWarnings.add(
                    "Data retrieval failed: " + rootMessage(e)
            );
        }

        return new OptimizationOutcome(
                d,
                original,
                finalSql,
                reasoning,
                flags,
                idx,
                settings,
                baselineCost,
                candidateCost,
                baselineMs,
                candidateMs,
                match,
                evidence,
                responseWarnings,
                retrievedData,
                dataTruncated
        );
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }

    private static String meta(Chunk c, String key) {
        Object v = c.metadata() == null ? null : c.metadata().get(key);
        return v == null ? "" : v.toString();
    }

    private static String cut(String s) {
        return s.length() <= MAX_CHUNK_CHARS ? s : s.substring(0, MAX_CHUNK_CHARS) + "...";
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        String m = c.getMessage();
        return m == null ? c.getClass().getSimpleName() : m.lines().findFirst().orElse(m);
    }
}
