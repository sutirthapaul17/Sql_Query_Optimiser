package org.example.queryoptimizer.Evaluation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    public record CompareRequest(String originalSql, String optimizedSql, Integer runs) {}

    @PostMapping("/compare")
    public ResponseEntity<ExperimentService.Comparison> compare(@RequestBody CompareRequest request) {
        int runs = request.runs() == null ? 3 : request.runs();
        return ResponseEntity.ok(
                experimentService.compare(request.originalSql(), request.optimizedSql(), runs));
    }
}
