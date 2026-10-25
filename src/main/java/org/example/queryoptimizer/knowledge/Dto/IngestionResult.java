package org.example.queryoptimizer.knowledge.Dto;

public record IngestionResult(
        String sourceUrl,
        String title,
        int chunksCreated,
        boolean successful,
        String message
) {}
