package org.example.queryoptimizer.Optimizer.Service.Impl;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;

import org.example.queryoptimizer.Optimizer.Dto.SqlAnalysisResult;
import org.example.queryoptimizer.Optimizer.Service.SqlAnalysisService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SqlAnalysisServiceImpl implements SqlAnalysisService {

    @Override
    public SqlAnalysisResult analyze(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL query must not be empty"
            );
        }

        Statement statement;

        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException ex) {
            throw new IllegalArgumentException(
                    "Invalid SQL syntax",
                    ex
            );
        }

        if (!(statement instanceof Select select)) {
            throw new IllegalArgumentException(
                    "Only SELECT queries are supported for analysis"
            );
        }

        if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
            throw new IllegalArgumentException(
                    "Only simple SELECT queries are currently supported. "
                            + "CTEs, set operations, and other query forms "
                            + "will be supported in a later version."
            );
        }

        List<String> tables = new ArrayList<>();
        List<String> joins = new ArrayList<>();
        List<String> filters = new ArrayList<>();
        List<String> groupByColumns = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Extract the main FROM table.
        FromItem fromItem = plainSelect.getFromItem();

        if (fromItem instanceof Table table) {
            tables.add(table.getFullyQualifiedName());
        } else if (fromItem != null) {
            warnings.add(
                    "The FROM clause contains a non-table source "
                            + "that is not yet extracted."
            );
        }

        // Extract JOIN clauses and their right-hand tables.
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                joins.add(join.toString());

                if (join.getRightItem() instanceof Table table) {
                    tables.add(table.getFullyQualifiedName());
                } else {
                    warnings.add(
                            "A joined subquery or non-table source "
                                    + "was not fully extracted."
                    );
                }
            }
        }

        // Extract WHERE.
        if (plainSelect.getWhere() != null) {
            filters.add(plainSelect.getWhere().toString());
        }

        // Extract GROUP BY expressions.
        GroupByElement groupBy = plainSelect.getGroupBy();

        if (groupBy != null
                && groupBy.getGroupByExpressionList() != null
                && groupBy.getGroupByExpressionList().getExpressions() != null) {

            groupBy.getGroupByExpressionList()
                    .getExpressions()
                    .forEach(expression ->
                            groupByColumns.add(expression.toString())
                    );
        }

        return new SqlAnalysisResult(
                List.copyOf(tables),
                List.copyOf(joins),
                List.copyOf(filters),
                List.copyOf(groupByColumns),
                List.copyOf(warnings)
        );
    }
}
