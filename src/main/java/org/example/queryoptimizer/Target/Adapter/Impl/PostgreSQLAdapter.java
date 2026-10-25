package org.example.queryoptimizer.Target.Adapter.Impl;

import org.example.queryoptimizer.Target.Adapter.DatabaseAdapter;
import org.example.queryoptimizer.Target.Dto.QueryPlanDto;
import org.example.queryoptimizer.Target.Parser.PostgreSQLPlanParser;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
//@RequiredArgsConstructor
public class PostgreSQLAdapter implements DatabaseAdapter {

    private final JdbcTemplate jdbcTemplate;
    private final PostgreSQLPlanParser planParser;

    public PostgreSQLAdapter(
            @Qualifier("targetJdbcTemplate") JdbcTemplate jdbcTemplate,
            PostgreSQLPlanParser planParser) {
        this.jdbcTemplate = jdbcTemplate;
        this.planParser = planParser;
    }

    @Override
    public QueryPlanDto getBaselinePlan(String validatedSql) {

        if (validatedSql == null || validatedSql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL query must not be empty"
            );
        }

        String explainSql =
                "EXPLAIN (FORMAT JSON) " + validatedSql;

        String planJson = jdbcTemplate.queryForObject(
                explainSql,
                (resultSet, rowNumber) -> resultSet.getString(1)
        );

        if (planJson == null || planJson.isBlank()) {
            throw new IllegalStateException(
                    "PostgreSQL returned an empty execution plan"
            );
        }

        return planParser.parse(planJson, "EXPLAIN");
    }
}
