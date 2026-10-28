package org.example.queryoptimizer.Optimizer.Engine;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Optimizer.Dto.Proposal;
import org.example.queryoptimizer.knowledge.Tools.OptimizationKnowledgeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Asks the LLM for a decision and, when it sees a better formulation, a candidate query. */
@Component
@RequiredArgsConstructor
public class CandidateGenerator {

    private static final String SYSTEM = """
            You are a PostgreSQL query optimization assistant. You receive a SELECT query, a summary of its
            baseline EXPLAIN plan from the PostgreSQL planner, schema information, and retrieved evidence.
 
            Rules:
            1. The baseline plan comes from PostgreSQL's own planner and is often already good. If the evidence
               does not clearly show a better formulation, answer KEEP_ORIGINAL. Never change a query just to change it.
            2. A rewritten query must return exactly the same rows and the same columns, in the same order of
               columns, as the original. Do not change its meaning. Only SELECT or WITH ... SELECT statements.
            3. Use only tables and columns that appear in the schema section.
            4. Index and setting suggestions are recommendations only. They are never executed.
            5. Documentation excerpts explain how PostgreSQL behaves. Past observations show how similar baseline
               plans looked and performed. They do not contain fixes, so do not treat them as proven rewrites.
 
            Answer with ONLY one JSON object, no markdown fences, in this form:
            {"decision":"KEEP_ORIGINAL" or "OPTIMIZE",
             "reasoning":"short explanation",
             "optimizedSql":"the rewritten query, or empty when decision is KEEP_ORIGINAL",
             "indexRecommendations":["CREATE INDEX ..."],
             "settingRecommendations":["SET ..."],
             "evidence":["which excerpts you relied on"]}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper mapper;
    private final OptimizationKnowledgeTool optimizationKnowledgeTool;
    private final PromptLoader promptLoader;

    /** @param feedback why the previous candidate was rejected, or null on the first attempt */
    public Proposal propose(String userPrompt, String previousCandidate, String feedback) {
        String user = userPrompt;
        if (feedback != null) {
            user += "\n\nYour previous candidate was rejected.\nCandidate:\n" + previousCandidate
                    + "\nReason: " + feedback
                    + "\nReturn a corrected candidate, or choose KEEP_ORIGINAL.";
        }
        String optimizationPolicy = promptLoader.load(
                "Prompts/Sql_optimization-Policy.txt"
        );
        String retrievalPolicy = promptLoader.load(
                "Prompts/sql-optimization-retrieval-policy.txt"
        );
        String completeSystemPrompt = SYSTEM
                + "\n\n"
                + optimizationPolicy
                + "\n\n"
                + retrievalPolicy;
        String raw = chatClient.prompt().system(SYSTEM).user(user).tools(optimizationKnowledgeTool).call().content();
        return parse(raw);
    }

    @SuppressWarnings("unchecked")
    Proposal parse(String raw) {
        if (raw == null) throw new IllegalStateException("The model returned no text");
        int a = raw.indexOf('{');
        int b = raw.lastIndexOf('}');
        if (a < 0 || b <= a) throw new IllegalStateException("The model did not return a JSON object");
        Map<String, Object> m;
        try {
            m = (Map<String, Object>) mapper.readValue(raw.substring(a, b + 1), Map.class);
        } catch (RuntimeException e) {
            throw new IllegalStateException("The model returned invalid JSON");
        }
        boolean optimize = "OPTIMIZE".equalsIgnoreCase(text(m.get("decision")));
        return new Proposal(optimize, text(m.get("reasoning")), text(m.get("optimizedSql")),
                strings(m.get("indexRecommendations"), "create index", "create unique index"),
                strings(m.get("settingRecommendations"), "set "),
                strings(m.get("evidence")));
    }

    private static String text(Object o) {
        return o == null ? "" : o.toString().strip();
    }

    private static List<String> strings(Object o, String... allowedPrefixes) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                String s = text(x);
                if (s.isBlank() || s.length() > 400) continue;
                if (allowedPrefixes.length > 0) {
                    String lower = s.toLowerCase(Locale.ROOT);
                    boolean ok = false;
                    for (String p : allowedPrefixes) ok |= lower.startsWith(p);
                    if (!ok) continue;
                }
                out.add(s);
            }
        }
        return out;
    }
}
