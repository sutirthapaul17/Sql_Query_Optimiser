package org.example.queryoptimizer.Optimizer.Dto;

import java.util.List;

public record SqlAnalysisResult(
        List<String> tables,
        List<String> joins,
        List<String> filters,
        List<String> groupByColumns,
        List<String> warnings
) { }
