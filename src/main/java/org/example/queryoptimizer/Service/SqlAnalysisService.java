package org.example.queryoptimizer.Service;

import org.example.queryoptimizer.Dto.response.SqlAnalysisResult;

public interface SqlAnalysisService {
    SqlAnalysisResult analyze(String sql);
}
