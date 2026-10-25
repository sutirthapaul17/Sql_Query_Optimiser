package org.example.queryoptimizer.Ingestion;


import org.example.queryoptimizer.knowledge.Config.PostgresDocumentationSources;
import org.example.queryoptimizer.knowledge.Dto.IngestionResult;
import org.example.queryoptimizer.knowledge.Ingestion.DocumentationIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class DocumentationIngestionIntegrationTest {

    @Autowired
    private DocumentationIngestionService ingestionService;

    @Test
    void ingestPostgreSQLDocumentation() {

        List<IngestionResult> results =
                ingestionService.ingest(
                        PostgresDocumentationSources.all()
                );

        results.forEach(System.out::println);

        assertFalse(results.isEmpty(),
                "No documentation sources were processed");
    }
}
