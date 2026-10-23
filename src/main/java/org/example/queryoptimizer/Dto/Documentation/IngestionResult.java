package org.example.queryoptimizer.Dto.Documentation;

public record IngestionResult(
        String sourceUrl,
        String title,
        int chunksCreated,
        boolean successful,
        String message
) {}
