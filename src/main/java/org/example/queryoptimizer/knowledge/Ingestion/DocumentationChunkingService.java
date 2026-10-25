package org.example.queryoptimizer.knowledge.Ingestion;


import org.example.queryoptimizer.knowledge.Dto.DocumentationPage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DocumentationChunkingService {

    private static final int MAX_CHUNK_CHARS = 3000;

    public List<Document> chunk(DocumentationPage page) {

        List<String> textChunks = splitText(page.content());
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", page.title());
            metadata.put("sourceUrl", page.sourceUrl());
            metadata.put("topic", page.topic());
            metadata.put("sourceType", "DOCUMENTATION");
            metadata.put("chunkIndex", i);

            documents.add(new Document(
                    textChunks.get(i),
                    metadata
            ));
        }

        return documents;
    }

    private List<String> splitText(String content) {

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : content.split("\\n\\s*\\n")) {

            paragraph = paragraph.trim();

            if (paragraph.isBlank()) {
                continue;
            }

            // Split unusually long paragraphs into smaller pieces.
            if (paragraph.length() > MAX_CHUNK_CHARS) {

                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }

                for (int start = 0;
                     start < paragraph.length();
                     start += MAX_CHUNK_CHARS) {

                    int end = Math.min(
                            start + MAX_CHUNK_CHARS,
                            paragraph.length()
                    );

                    chunks.add(paragraph.substring(start, end));
                }

                continue;
            }

            if (!current.isEmpty()
                    && current.length() + paragraph.length() + 2
                    > MAX_CHUNK_CHARS) {

                chunks.add(current.toString().trim());
                current.setLength(0);
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }

            current.append(paragraph);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}
