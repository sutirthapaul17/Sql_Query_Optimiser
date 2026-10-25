package org.example.queryoptimizer.Common;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {}
