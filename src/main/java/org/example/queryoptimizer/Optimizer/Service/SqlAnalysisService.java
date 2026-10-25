package org.example.queryoptimizer.Optimizer.Service;

import org.example.queryoptimizer.Optimizer.Dto.SqlAnalysisResult;

public interface SqlAnalysisService {
    SqlAnalysisResult analyze(String sql);
}
