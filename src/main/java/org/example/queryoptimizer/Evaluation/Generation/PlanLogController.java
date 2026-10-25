package org.example.queryoptimizer.Evaluation.Generation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/experiments")
public class PlanLogController {

    private final PlanLogGenerator generator;

    public PlanLogController(PlanLogGenerator generator) {
        this.generator = generator;
    }

    /** analyze=false: plan only (safe, fast). analyze=true: really executes each query. */
    @PostMapping("/plan-log")
    public PlanLogGenerator.Summary generate(@RequestParam(defaultValue = "false") boolean analyze)
            throws IOException {
        return generator.generate(analyze);
    }
}