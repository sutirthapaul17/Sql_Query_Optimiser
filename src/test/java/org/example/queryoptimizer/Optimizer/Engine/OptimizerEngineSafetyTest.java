package org.example.queryoptimizer.Optimizer.Engine;

import org.example.queryoptimizer.Common.PlanFacts;
import org.example.queryoptimizer.Optimizer.Dto.OptimizationOutcome;
import org.example.queryoptimizer.Optimizer.Dto.Proposal;
import org.example.queryoptimizer.Target.Service.QueryResultService;
import org.example.queryoptimizer.Target.Tools.TargetDatabaseTool;
import org.example.queryoptimizer.knowledge.Tools.KnowledgeSearchTool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptimizerEngineSafetyTest {

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
                0.05,
                false,
                false,
                2,
                2,
                4
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DELETE FROM customer",
            "UPDATE customer SET c_name = 'changed'",
            "INSERT INTO customer VALUES (1)",
            "DROP TABLE customer",
            "SELECT 1; DROP TABLE customer",
            "SELECT 1; SELECT 2"
    })
    void rejectsUnsafeOrMultipleStatementsBeforeDatabaseAccess(
            String sql
    ) {
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
    void rejectsNullAndBlankSqlBeforeDatabaseAccess() {
        // Null SQL
        OptimizationOutcome nullOutcome = engine.optimize(null);

        assertEquals(
                OptimizationOutcome.Decision.FAILED,
                nullOutcome.decision()
        );

        assertTrue(
                nullOutcome.reasoning().contains(
                        "SQL must not be empty"
                )
        );

        // Blank SQL
        OptimizationOutcome blankOutcome = engine.optimize("   ");

        assertEquals(
                OptimizationOutcome.Decision.FAILED,
                blankOutcome.decision()
        );

        assertTrue(
                blankOutcome.reasoning().contains(
                        "SQL must not be empty"
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
    void acceptsSingleSelectForBaselinePlanning() {
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
                any(PlanFacts.class),
                anyMap()
        )).thenReturn(List.of());

        // Act
        OptimizationOutcome outcome = engine.optimize(sql);

        // Assert
        assertEquals(
                OptimizationOutcome.Decision.KEEP_ORIGINAL,
                outcome.decision()
        );

        verify(target).explain(sql);
        verify(assessor).assess(
                eq(sql),
                any(PlanFacts.class),
                anyMap()
        );

        verifyNoInteractions(search, generator);
    }

    @Test
    void rejectsUnsafeLlmCandidateWithoutExplainingOrExecutingIt() {
        // Arrange
        String originalSql =
                "SELECT c_custkey FROM customer";

        String unsafeCandidate =
                "DELETE FROM customer";

        Map<String, Object> plan = Map.of(
                "Node Type", "Seq Scan",
                "Relation Name", "customer",
                "Plan Rows", 100,
                "Total Cost", 100.0
        );

        when(target.explain(originalSql))
                .thenReturn(Map.of("Plan", plan));

        when(target.rowEstimates(anyCollection()))
                .thenReturn(Map.of());

        when(assessor.assess(
                eq(originalSql),
                any(PlanFacts.class),
                anyMap()
        )).thenReturn(List.of(
                new PlanAssessor.Flag(
                        "LARGE_SCAN",
                        "A potentially expensive scan was detected",
                        List.of("QUERY_PLANNER")
                )
        ));

        when(target.schemaContext(anyCollection(), anyMap()))
                .thenReturn("customer table schema");

        when(search.documentation(
                anyString(),
                anyList(),
                anyInt()
        )).thenReturn(List.of());

        when(search.similarPlans(anyString(), anyInt()))
                .thenReturn(List.of());

        Proposal unsafeProposal = new Proposal(
                true,
                "Attempted unsafe rewrite",
                unsafeCandidate,
                List.of(),
                List.of(),
                List.of()
        );

        when(generator.propose(
                anyString(),
                nullable(String.class),
                nullable(String.class)
        )).thenReturn(unsafeProposal);

        // Act
        OptimizationOutcome outcome = engine.optimize(originalSql);

        // Assert
        assertEquals(
                OptimizationOutcome.Decision.KEEP_ORIGINAL,
                outcome.decision()
        );

        assertEquals(originalSql, outcome.finalSql());

        assertTrue(
                outcome.warnings().stream().anyMatch(
                        warning -> warning.contains("candidate rejected")
                )
        );

        // The original query may be explained, but the unsafe candidate
        // must never be passed to PostgreSQL for planning or execution.
        verify(target).explain(originalSql);
        verify(target, never()).explain(unsafeCandidate);
        verify(target, never()).analyze(unsafeCandidate);
        verify(target, never()).fingerprint(unsafeCandidate);
    }
}

