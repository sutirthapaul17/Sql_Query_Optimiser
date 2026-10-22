package org.example.queryoptimizer.Service;

import org.example.queryoptimizer.Dto.request.QueryAnalysisRequestDto;
import org.example.queryoptimizer.Dto.response.QueryAnalysisResponseDto;

public interface QueryAnalysisService {

    QueryAnalysisResponseDto analyze(
            QueryAnalysisRequestDto request
    );
}
