package org.example.queryoptimizer.knowledge.Tools;

import org.example.queryoptimizer.knowledge.KnowledgeRetriever.Chunk;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OptimizationKnowledgeTool {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 5;
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_CHUNK_CHARS = 1800;

    private final KnowledgeSearchTool knowledgeSearchTool;

    public OptimizationKnowledgeTool(
            KnowledgeSearchTool knowledgeSearchTool) {
        this.knowledgeSearchTool = knowledgeSearchTool;
    }

    @Tool(
            name = "searchOptimizationKnowledge",
            description = """
            Search the PostgreSQL optimization documentation for
            additional evidence needed to evaluate a SQL rewrite.

            Use this tool when the initial prompt does not contain
            enough information about PostgreSQL behavior, especially
            NULL semantics, NOT IN versus NOT EXISTS, correlated
            subqueries, joins, indexes, data types, or query correctness.

            Provide a specific question, relevant topic names, and
            the desired number of results. This tool searches
            documentation only. It does not execute SQL.
            """
    )
    public String searchOptimizationKnowledge(
            @ToolParam(
                    description = "A focused question about the PostgreSQL behavior being investigated"
            )
            String query,

            @ToolParam(
                    description = "Relevant documentation topic names, such as QUERY_REWRITING, JOINS, or INDEXES"
            )
            List<String> topics,

            @ToolParam(
                    description = "Number of documentation chunks to retrieve, from 1 to 5"
            )
            Integer topK) {

        if (query == null || query.isBlank()) {
            return "No documentation retrieved: query must not be blank.";
        }

        String safeQuery = query.strip();

        if (safeQuery.length() > MAX_QUERY_LENGTH) {
            safeQuery = safeQuery.substring(0, MAX_QUERY_LENGTH);
        }

        int limit = topK == null
                ? DEFAULT_TOP_K
                : Math.max(1, Math.min(topK, MAX_TOP_K));

        List<String> safeTopics = topics == null
                ? List.of()
                : topics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(String::strip)
                .distinct()
                .limit(8)
                .toList();

        List<Chunk> results = knowledgeSearchTool.documentation(
                safeQuery,
                safeTopics,
                limit
        );

        if (results.isEmpty()) {
            return """
                No matching documentation was found for this search.
                Do not invent evidence. Try a more specific query or
                different relevant topic names if additional evidence
                is still necessary.
                """;
        }

        StringBuilder response = new StringBuilder();
        response.append("Retrieved ")
                .append(results.size())
                .append(" documentation chunk(s):\n\n");

        for (int i = 0; i < results.size(); i++) {
            Chunk chunk = results.get(i);

            String content = chunk.content() == null
                    ? ""
                    : chunk.content();

            if (content.length() > MAX_CHUNK_CHARS) {
                content = content.substring(0, MAX_CHUNK_CHARS)
                        + "\n[Excerpt truncated]";
            }

            Map<String, Object> metadata = chunk.metadata();

            response.append("--- DOCUMENT ")
                    .append(i + 1)
                    .append(" ---\n");

            if (metadata != null && !metadata.isEmpty()) {
                response.append("Metadata: ")
                        .append(metadata.entrySet().stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(", ")))
                        .append("\n");
            }

            response.append(content)
                    .append("\n\n");
        }

        return response.toString();
    }
}
