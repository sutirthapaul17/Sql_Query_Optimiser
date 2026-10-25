package org.example.queryoptimizer.knowledge.Controller;

import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.knowledge.Config.PostgresDocumentationSources;
import org.example.queryoptimizer.knowledge.Dto.DocumentationSource;
import org.example.queryoptimizer.knowledge.Dto.IngestionResult;
import org.example.queryoptimizer.knowledge.Ingestion.DocumentationIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class IngestionController {

    private final DocumentationIngestionService ingestionService;

    @PostMapping("/ingest")
    public List<IngestionResult> ingest(@RequestParam(defaultValue = "0") int limit) {
        List<DocumentationSource> sources = PostgresDocumentationSources.all();
        if (limit > 0 && limit < sources.size()) {
            sources = sources.subList(0, limit);
        }
        return ingestionService.ingest(sources);
    }
}