
package org.example.queryoptimizer.Optimizer.Engine;

import org.example.queryoptimizer.Optimizer.Dto.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Engine.PlanAssessor.Flag;
import org.example.queryoptimizer.Target.Service.QueryResultService;
import org.example.queryoptimizer.Target.Tools.TargetDatabaseTool;
import org.example.queryoptimizer.knowledge.Tools.KnowledgeSearchTool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptimizerEngineTest {

    @Mock
    private TargetDatabaseTool target;

    @Mock
    private PlanAssessor assessor;

    @Mock
    private KnowledgeSearchTool search;

    @Mock
    private CandidateGenerator generator;

    @Mock
    private QueryResultService queryResultService;

    private OptimizerEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OptimizerEngine(
                target,
                assessor,
                search,
                generator,
                queryResultService,
                0.05,   // Minimum estimated-cost improvement: 5%
                false,  // Do not verify execution time yet
                false,  // Do not always consult the LLM
                2,      // Maximum candidate attempts
                2,      // Documents per flag
                4       // Similar plans to retrieve
        );
    }

    @Test
    void rejectsNonSelectBeforeCallingAnyPipelineDependency() {
        // Arrange
        String sql = "DELETE FROM customer";

        // Act
        OptimizationOutcome outcome = engine.optimize(sql);

        // Assert
        assertEquals(
                OptimizationOutcome.Decision.FAILED,
                outcome.decision()
        );

        assertTrue(
                outcome.reasoning().contains(
                        "Only a single SELECT query is allowed"
                )
        );

        verifyNoInteractions(
                target,
                assessor,
                search,
                generator,
                queryResultService
        );
    }

    @Test
    void returnsFailedOutcomeWhenBaselineExplainFails() {
        // Arrange
        String sql = "SELECT c_custkey FROM customer";

        when(target.explain(sql))
                .thenThrow(
                        new IllegalStateException("database unavailable")
                );

        // Act
        OptimizationOutcome outcome = engine.optimize(sql);

        // Assert
        assertEquals(
                OptimizationOutcome.Decision.FAILED,
                outcome.decision()
        );

        assertTrue(
                outcome.reasoning().contains(
                        "could not explain the query"
                )
        );

        verify(target).explain(sql);

        verifyNoInteractions(
                assessor,
                search,
                generator,
                queryResultService
        );
    }

    @Test
    void keepsOriginalAndSkipsKnowledgeAndLlmWhenBaselineHasNoFlags() {
        // Arrange
        String sql = "SELECT c_custkey FROM customer";

        Map<String, Object> plan = Map.of(
                "Node Type", "Seq Scan",
                "Relation Name", "customer",
                "Plan Rows", 100,
                "Total Cost", 10.0
        );

        when(target.explain(sql))
                .thenReturn(Map.of("Plan", plan));

        when(target.rowEstimates(anyCollection()))
                .thenReturn(Map.of());

        when(assessor.assess(
                eq(sql),
                any(),
                anyMap()
        )).thenReturn(List.<Flag>of());

        when(queryResultService.execute(sql))
                .thenReturn(
                        new QueryResultService.QueryResult(
                                List.of(),
                                false
                        )
                );

        // Act
        OptimizationOutcome outcome = engine.optimize(sql);

        // Assert
        assertEquals(
                OptimizationOutcome.Decision.KEEP_ORIGINAL,
                outcome.decision()
        );

        assertEquals(sql, outcome.originalSql());
        assertEquals(sql, outcome.finalSql());

        verify(target).explain(sql);
        verify(assessor).assess(eq(sql), any(), anyMap());
        verify(queryResultService).execute(sql);

        verifyNoInteractions(search, generator);
    }
}
