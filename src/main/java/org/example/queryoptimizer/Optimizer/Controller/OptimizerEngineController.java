package org.example.queryoptimizer.Optimizer.Controller;

import org.example.queryoptimizer.Optimizer.Engine.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Engine.OptimizerEngine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Direct access to the engine with the full structured result. Useful for testing and evaluation. */
@RestController
@RequestMapping("/api/optimizer")
public class OptimizerEngineController {

    private final OptimizerEngine engine;

    public OptimizerEngineController(OptimizerEngine engine) {
        this.engine = engine;
    }

    public record Request(String sql) {}

    @PostMapping("/engine")
    public OptimizationOutcome optimize(@RequestBody Request request) {
        return engine.optimize(request.sql());
    }
}

