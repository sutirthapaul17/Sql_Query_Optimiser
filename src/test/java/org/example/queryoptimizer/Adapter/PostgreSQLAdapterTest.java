package org.example.queryoptimizer.Adapter;


import org.example.queryoptimizer.Adapter.Impl.PostgreSQLAdapter;
import org.example.queryoptimizer.Dto.QueryPlanDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostgreSQLAdapterTest {

    @Autowired
    private PostgreSQLAdapter postgreSQLAdapter;

    @Test
    void shouldRetrieveBaselinePlanFromPostgreSQL() {
        String sql = """
                SELECT c.c_name, SUM(o.o_totalprice)
                FROM customer c
                JOIN orders o
                    ON o.o_custkey = c.c_custkey
                WHERE c.c_mktsegment = 'BUILDING'
                GROUP BY c.c_name
                """;

        QueryPlanDto plan = postgreSQLAdapter.getBaselinePlan(sql);

        assertNotNull(plan);
        assertEquals("EXPLAIN", plan.explainMode());
        assertNotNull(plan.rootPlan());
        assertNotNull(plan.rootPlan().nodeType());

        System.out.println("Baseline plan: " + plan);
    }
}
