package org.example.queryoptimizer.Service;


import org.example.queryoptimizer.Dto.response.SqlAnalysisResult;
import org.example.queryoptimizer.Service.Impl.SqlAnalysisServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlAnalysisServiceImplTest {

    private final SqlAnalysisServiceImpl service =
            new SqlAnalysisServiceImpl();

    @Test
    void shouldExtractTablesFiltersAndGroupBy() {
        String sql = """
                SELECT c.c_name, SUM(o.o_totalprice)
                FROM customer c
                JOIN orders o
                    ON o.o_custkey = c.c_custkey
                WHERE c.c_mktsegment = 'BUILDING'
                GROUP BY c.c_name
                """;

        SqlAnalysisResult result = service.analyze(sql);

        assertEquals(2, result.tables().size());
        assertTrue(result.tables().contains("customer"));
        assertTrue(result.tables().contains("orders"));

        assertEquals(1, result.joins().size());
        assertEquals(1, result.filters().size());

        assertTrue(result.filters().get(0)
                .contains("BUILDING"));

        assertEquals(
                java.util.List.of("c.c_name"),
                result.groupByColumns()
        );
    }

    @Test
    void shouldRejectInvalidSql() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze("SELECT FROM WHERE")
        );
    }

    @Test
    void shouldRejectNonSelectStatements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze("DELETE FROM customer")
        );
    }
}
