package org.example.queryoptimizer.Optimizer.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationResponseDto;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisRequestDto;
import org.example.queryoptimizer.Common.ApiResponse;
import org.example.queryoptimizer.Optimizer.Dto.QueryAnalysisResponseDto;
import org.example.queryoptimizer.Optimizer.Service.QueryAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query-analysis")
@RequiredArgsConstructor
public class QueryAnalysisController {

    private final QueryAnalysisService queryAnalysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<OptimizationResponseDto>>
    analyzeQuery(
            @Valid @RequestBody QueryAnalysisRequestDto request
    ) {
        OptimizationResponseDto response =
                queryAnalysisService.analyze(request);

        ApiResponse<OptimizationResponseDto> apiResponse =
                new ApiResponse<>(
                        true,
                        "Query analysis and optimization completed",
                        response
                );

        return ResponseEntity.ok(apiResponse);
    }
}
