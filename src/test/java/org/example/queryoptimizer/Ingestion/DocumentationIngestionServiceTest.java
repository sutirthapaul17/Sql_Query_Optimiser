//package org.example.queryoptimizer.Ingestion;
//
//
//import org.example.queryoptimizer.Service.DocumentationService.DocumentationIngestionService;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class DocumentationIngestionServiceTest {
//
//    @Test
//    void shouldFetchPostgresExplainDocumentation()
//            throws Exception {
//
//        var service = new DocumentationIngestionService();
//
//        var page = service.fetchDocumentation();
//
//        assertNotNull(page.title());
//        assertEquals(
//                "https://www.postgresql.org/docs/current/using-explain.html",
//                page.sourceUrl()
//        );
//        assertFalse(page.content().isBlank());
//
//        System.out.println("Title: " + page.title());
//        System.out.println("Source: " + page.sourceUrl());
//        System.out.println(
//                "Extracted characters: " + page.content().length()
//        );
//        System.out.println(
//                page.content().substring(
//                        0, Math.min(1000, page.content().length())
//                )
//        );
//    }
//}
