package org.example.queryoptimizer.knowledge;

import org.example.queryoptimizer.knowledge.KnowledgeRetriever.Chunk;
import org.example.queryoptimizer.knowledge.Tools.KnowledgeSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KnowledgeRetrievalRelevanceIT {

    @Autowired
    private KnowledgeSearchTool knowledgeSearchTool;

    private static final Path RESULTS_FILE =
            Path.of("evaluation-results", "relevance-results.csv");

    record RetrievalCase(
            String question,
            Set<String> relevantTopics
    ) {}

    @Test
    void retrievesRelevantDocumentationForSqlOptimizationQuestions()
            throws IOException {

        // All questions in this test execution share the same run ID.
        String runId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        List<RetrievalCase> cases = List.of(
                new RetrievalCase(
                        "When should PostgreSQL use an index scan instead "
                                + "of a sequential scan for a selective filter?",
                        Set.of(
                                "INDEXES",
                                "INDEX_USAGE",
                                "QUERY_PLANNER"
                        )
                ),
                new RetrievalCase(
                        "How can inaccurate row-count estimates affect "
                                + "PostgreSQL join planning and query cost?",
                        Set.of(
                                "STATISTICS",
                                "QUERY_PLANNER",
                                "EXECUTION_PLANS"
                        )
                ),
                new RetrievalCase(
                        "How do I inspect an execution plan to identify "
                                + "an expensive join or sequential scan?",
                        Set.of(
                                "EXECUTION_PLANS",
                                "QUERY_PLANNER"
                        )
                ),
                new RetrievalCase(
                        "What should I consider when choosing the order "
                                + "of columns in a composite index?",
                        Set.of(
                                "INDEXES",
                                "INDEX_USAGE"
                        )
                )
        );

        int totalRelevant = 0;
        int totalRetrieved = 0;
        int totalExpected = 0;

        for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
            RetrievalCase testCase = cases.get(caseIndex);
            String questionId = "Q" + (caseIndex + 1);

            // Search the whole knowledge store without topic filtering.
            List<Chunk> results =
                    knowledgeSearchTool.anything(
                            testCase.question(),
                            5
                    );

            assertFalse(
                    results.isEmpty(),
                    "No chunks retrieved for: " + testCase.question()
                            + ". Check whether documentation has been ingested."
            );

            long relevantCount = results.stream()
                    .filter(chunk -> hasRelevantTopic(
                            chunk, testCase.relevantTopics()))
                    .count();

            double questionPrecision =
                    (double) relevantCount / results.size();

            totalRelevant += (int) relevantCount;
            totalRetrieved += results.size();
            totalExpected += testCase.relevantTopics().size();

            System.out.println("\nQuestion: " + testCase.question());
            System.out.println("Expected relevant topics: "
                    + testCase.relevantTopics());

            for (int i = 0; i < results.size(); i++) {
                Chunk chunk = results.get(i);

                System.out.printf(
                        "Rank %d | title=%s | topic=%s | sourceType=%s%n",
                        i + 1,
                        metadata(chunk, "title"),
                        metadata(chunk, "topic"),
                        metadata(chunk, "sourceType")
                );
            }

            System.out.println("Relevant results in top 5: "
                    + relevantCount + "/" + results.size());

            // Append the per-question result before the assertion.
            appendResult(
                    timestamp,
                    runId,
                    questionId,
                    testCase.question(),
                    5,
                    (int) relevantCount,
                    questionPrecision,
                    testCase.relevantTopics().stream()
                            .sorted()
                            .collect(Collectors.joining("|")),
                    results.stream()
                            .map(chunk -> metadata(chunk, "topic"))
                            .collect(Collectors.joining("|"))
            );

            assertTrue(
                    relevantCount > 0,
                    "No relevant document appeared in the top 5 for: "
                            + testCase.question()
                            + ". Inspect the printed results."
            );
        }

        double precisionAt5 =
                totalRetrieved == 0
                        ? 0.0
                        : (double) totalRelevant / totalRetrieved;

        System.out.printf(
                "%nOverall Precision@5: %.2f%%%n",
                precisionAt5 * 100
        );

        // Append an aggregate row for this complete test execution.
        appendResult(
                timestamp,
                runId,
                "OVERALL",
                "Overall retrieval relevance",
                5,
                totalRelevant,
                precisionAt5,
                "Expected topics across " + cases.size() + " questions",
                "Retrieved chunks=" + totalRetrieved
        );

        System.out.println("Results appended to: "
                + RESULTS_FILE.toAbsolutePath());
    }

    private static synchronized void appendResult(
            String timestamp,
            String runId,
            String questionId,
            String question,
            int topK,
            int relevantCount,
            double precisionAtK,
            String expectedTopics,
            String retrievedTopics
    ) throws IOException {

        Files.createDirectories(RESULTS_FILE.getParent());

        boolean writeHeader =
                !Files.exists(RESULTS_FILE)
                        || Files.size(RESULTS_FILE) == 0;

        StringBuilder csv = new StringBuilder();

        if (writeHeader) {
            csv.append(
                    "timestamp,run_id,question_id,question,top_k,"
                            + "relevant_count,precision_at_k,"
                            + "expected_topics,retrieved_topics\n"
            );
        }

        csv.append(csvEscape(timestamp)).append(',')
                .append(csvEscape(runId)).append(',')
                .append(csvEscape(questionId)).append(',')
                .append(csvEscape(question)).append(',')
                .append(topK).append(',')
                .append(relevantCount).append(',')
                .append(String.format(
                        Locale.ROOT, "%.4f", precisionAtK
                )).append(',')
                .append(csvEscape(expectedTopics)).append(',')
                .append(csvEscape(retrievedTopics)).append('\n');

        Files.writeString(
                RESULTS_FILE,
                csv.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static boolean hasRelevantTopic(
            Chunk chunk,
            Set<String> relevantTopics
    ) {
        Object topic = chunk.metadata() == null
                ? null
                : chunk.metadata().get("topic");

        return topic != null
                && relevantTopics.contains(topic.toString());
    }

    private static String metadata(Chunk chunk, String key) {
        Map<String, Object> metadata = chunk.metadata();

        if (metadata == null) {
            return "missing";
        }

        Object value = metadata.get(key);
        return value == null ? "missing" : value.toString();
    }
}