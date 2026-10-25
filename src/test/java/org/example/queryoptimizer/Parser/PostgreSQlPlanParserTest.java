package org.example.queryoptimizer.Parser;

import org.example.queryoptimizer.Target.Dto.QueryPlanDto;
import org.example.queryoptimizer.Target.Parser.PostgreSQLPlanParser;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSQLPlanParserTest {

    private final PostgreSQLPlanParser parser =
            new PostgreSQLPlanParser(new ObjectMapper());

    @Test
    void shouldParseSequentialScanPlan() {
        String json = """
                [
                  {
                    "Plan": {
                      "Node Type": "Seq Scan",
                      "Relation Name": "customer",
                      "Alias": "c",
                      "Startup Cost": 0.0,
                      "Total Cost": 20.0,
                      "Plan Rows": 100,
                      "Plan Width": 32
                    }
                  }
                ]
                """;

        QueryPlanDto result = parser.parse(json, "EXPLAIN");

        assertEquals("EXPLAIN", result.explainMode());
        assertNotNull(result.rootPlan());
        assertEquals("Seq Scan", result.rootPlan().nodeType());
        assertEquals("customer", result.rootPlan().relationName());
        assertEquals(20.0, result.rootPlan().totalCost());
        assertTrue(result.rootPlan().children().isEmpty());

        // Plain EXPLAIN does not provide actual execution metrics.
        assertNull(result.executionTimeMs());
        assertNull(result.rootPlan().actualRows());
    }
}
