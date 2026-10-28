package org.example.queryoptimizer.Optimizer.Controller;


import org.example.queryoptimizer.Optimizer.Dto.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Engine.OptimizerEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptimizerEngineControllerTest {

    @Mock
    private OptimizerEngine engine;

    @InjectMocks
    private OptimizerEngineController controller;

    @Test
    void optimizeDelegatesSqlToEngineAndReturnsItsOutcome() {
        // Arrange
        String sql = "SELECT c_custkey FROM customer";

        OptimizationOutcome expected = new OptimizationOutcome(
                OptimizationOutcome.Decision.KEEP_ORIGINAL,
                sql,
                sql,
                "Baseline plan looks acceptable",
                List.of(),
                List.of(),
                List.of(),
                100.0,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                false
        );

        when(engine.optimize(sql)).thenReturn(expected);

        // Act
        OptimizationOutcome actual = controller.optimize(
                new OptimizerEngineController.Request(sql)
        );

        // Assert
        assertSame(
                expected,
                actual,
                "Controller should return the engine outcome unchanged"
        );

        verify(engine).optimize(sql);
    }
}

