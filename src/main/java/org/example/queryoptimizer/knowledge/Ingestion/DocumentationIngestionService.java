package org.example.queryoptimizer.knowledge.Ingestion;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.knowledge.Dto.DocumentationSource;
import org.example.queryoptimizer.knowledge.Dto.IngestionResult;
import org.example.queryoptimizer.knowledge.Store.DocumentationVectorStoreService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentationIngestionService {

    private final DocumentationFetcherService fetcherService;
    private final DocumentationChunkingService chunkingService;
    private final DocumentationVectorStoreService vectorStoreService;

    public List<IngestionResult> ingest(
            List<DocumentationSource> sources) {

        List<IngestionResult> results = new ArrayList<>();

        for (DocumentationSource source : sources) {
            try {
                var page = fetcherService.fetch(source);

                List<Document> chunks =
                        chunkingService.chunk(page);

                vectorStoreService.store(chunks);

                results.add(new IngestionResult(
                        source.url(),
                        page.title(),
                        chunks.size(),
                        true,
                        "Documentation ingested successfully"
                ));

            } catch (IOException | RuntimeException ex) {
                results.add(new IngestionResult(
                        source.url(),
                        source.title(),
                        0,
                        false,
                        "Ingestion failed: " + ex.getMessage()
                ));
            }
        }
        return results;
    }
}

