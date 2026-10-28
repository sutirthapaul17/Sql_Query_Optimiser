package org.example.queryoptimizer.Target.Service;

import org.example.queryoptimizer.Target.Tools.TargetDatabaseTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryResultService {

    private final JdbcTemplate jdbc;
    private final int maxRows;
    private final int queryTimeoutSeconds;

    public QueryResultService(
            @Qualifier("targetJdbcTemplate") JdbcTemplate jdbc,
            @Value("${app.optimizer.result-max-rows:100}") int maxRows,
            @Value("${app.optimizer.result-query-timeout-seconds:30}")
            int queryTimeoutSeconds) {

        this.jdbc = jdbc;
        this.maxRows = Math.max(1, maxRows);
        this.queryTimeoutSeconds = Math.max(1, queryTimeoutSeconds);
    }

    public record QueryResult(
            List<Map<String, Object>> rows,
            boolean truncated) {}

    public QueryResult execute(String rawSql) {
        String sql = TargetDatabaseTool.requireSingleSelect(rawSql);

        // Fetch one extra row to determine whether the response was truncated.
        int fetchLimit = maxRows + 1;

        List<Map<String, Object>> fetched = jdbc.query(
                (PreparedStatementCreator) connection -> {
                    var statement = connection.prepareStatement(sql);
                    statement.setMaxRows(fetchLimit);
                    statement.setQueryTimeout(queryTimeoutSeconds);
                    return statement;
                },
                (ResultSet rs) -> {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    ResultSetMetaData metadata = rs.getMetaData();
                    int columnCount = metadata.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();

                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metadata.getColumnLabel(i),
                                    rs.getObject(i));
                        }

                        rows.add(row);
                    }

                    return rows;
                });

        boolean truncated = fetched.size() > maxRows;

        List<Map<String, Object>> returnedRows = truncated
                ? List.copyOf(fetched.subList(0, maxRows))
                : List.copyOf(fetched);

        return new QueryResult(returnedRows, truncated);
    }
}

