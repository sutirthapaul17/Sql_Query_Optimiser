package org.example.queryoptimizer.knowledge.Tools;

import org.example.queryoptimizer.knowledge.KnowledgeRetriever.Chunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Semantic search over the knowledge store (ragdb), one method per search case.
 * It only reads the vector store. It never touches the target database.
 */
@Component
public class KnowledgeSearchTool {

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_ .#-]+");

    private final VectorStore vectorStore;

    public KnowledgeSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** Case 1: documentation about specific topics, such as JOINS or INDEXES. */
    public List<Chunk> documentation(String query, List<String> topics, int topK) {
        String filter = "sourceType == 'DOCUMENTATION'";
        List<String> safe = topics.stream().filter(KnowledgeSearchTool::safe).toList();
        if (!safe.isEmpty()) {
            filter += " && topic in [" + safe.stream().map(t -> "'" + t + "'").collect(Collectors.joining(", ")) + "]";
        }
        return run(query, topK, filter);
    }

    /** Case 2: past baseline observations that look like the given plan description. */
    public List<Chunk> similarPlans(String planDescription, int topK) {
        return run(planDescription, topK, "sourceType == 'PLAN_LOG'");
    }

    /** Case 3: the same, limited to one query category (scan, join, subquery, ...). */
    public List<Chunk> similarPlansInCategory(String planDescription, String category, int topK) {
        if (!safe(category)) return List.of();
        return run(planDescription, topK, "sourceType == 'PLAN_LOG' && category == '" + category + "'");
    }

    /** Case 4: every recorded formulation of one problem (queries sharing a group label, such as H2). */
    public List<Chunk> plansInGroup(String group) {
        if (!safe(group)) return List.of();
        return run("group " + group, 10, "sourceType == 'PLAN_LOG' && group == '" + group + "'");
    }

    /** Case 5: free search over everything in the store. */
    public List<Chunk> anything(String query, int topK) {
        return run(query, topK, null);
    }

    private List<Chunk> run(String query, int topK, String filter) {
        SearchRequest.Builder b = SearchRequest.builder().query(query).topK(topK);
        if (filter != null) {
            b.filterExpression(filter);
        }
        List<Document> docs = vectorStore.similaritySearch(b.build());
        return docs.stream().map(d -> new Chunk(d.getText(), d.getMetadata())).toList();
    }

    private static boolean safe(String s) {
        return s != null && SAFE.matcher(s).matches();
    }
}