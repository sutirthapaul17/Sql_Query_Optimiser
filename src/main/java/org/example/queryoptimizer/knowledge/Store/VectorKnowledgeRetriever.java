package org.example.queryoptimizer.knowledge.Store;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.knowledge.KnowledgeRetriever;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorKnowledgeRetriever implements KnowledgeRetriever {

    private final VectorStore vectorStore;

//    public VectorKnowledgeRetriever(VectorStore vectorStore) {
//        this.vectorStore = vectorStore;
//    }

    @Override
    public List<Chunk> retrieve(String question, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(topK).build());

        return docs.stream()
                .map(d -> new Chunk(d.getText(), d.getMetadata()))
                .toList();
    }
}
