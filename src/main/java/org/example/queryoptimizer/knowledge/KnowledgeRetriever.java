package org.example.queryoptimizer.knowledge;

import java.util.List;
import java.util.Map;

/**
 * Public entry point of the knowledge (RAG) module.
 * The optimizer depends on this interface only, never on the vector store directly.
 */
public interface KnowledgeRetriever {

    int DEFAULT_TOP_K = 5;

    List<Chunk> retrieve(String question, int topK);

    default List<Chunk> retrieve(String question) {
        return retrieve(question, DEFAULT_TOP_K);
    }

    record Chunk(String content, Map<String, Object> metadata) {}
}
