package org.example.queryoptimizer.Dto.response;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {}
