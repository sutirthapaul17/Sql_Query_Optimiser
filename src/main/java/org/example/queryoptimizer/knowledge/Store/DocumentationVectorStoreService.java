package org.example.queryoptimizer.knowledge.Store;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class DocumentationVectorStoreService {

    private final VectorStore vectorStore;

    public void store(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return;
        }

        // Spring AI generates embeddings and stores the documents.
        vectorStore.add(documents);
    }
}
