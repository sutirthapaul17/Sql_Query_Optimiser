package org.example.queryoptimizer.Parser;


import lombok.RequiredArgsConstructor;
import org.example.queryoptimizer.Dto.QueryPlanDto;
import org.example.queryoptimizer.Dto.QueryPlanNodeDto;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostgreSQLPlanParser {

    private final ObjectMapper objectMapper;


    public QueryPlanDto parse(String json, String explainMode) {
        JsonNode result = objectMapper.readTree(json);

        // PostgreSQL EXPLAIN FORMAT JSON returns an array.
        if (result == null || !result.isArray()
                || result.isEmpty()) {
            throw new IllegalStateException(
                    "PostgreSQL returned an empty execution plan"
            );
        }

        JsonNode statement = result.get(0);
        JsonNode plan = statement.get("Plan");

        if (plan == null || plan.isNull()) {
            throw new IllegalStateException(
                    "Execution plan does not contain a Plan node"
            );
        }

        return new QueryPlanDto(
                explainMode,
                nullableDouble(statement, "Planning Time"),
                nullableDouble(statement, "Execution Time"),
                parseNode(plan)
        );

    }

    private QueryPlanNodeDto parseNode(JsonNode node) {

        List<QueryPlanNodeDto> children = new ArrayList<>();
        JsonNode plans = node.get("Plans");

        if (plans != null && plans.isArray()) {
            for (JsonNode child : plans) {
                children.add(parseNode(child));
            }
        }

        return new QueryPlanNodeDto(
                textValue(node, "Node Type"),
                textValue(node, "Relation Name"),
                textValue(node, "Alias"),
                textValue(node, "Index Name"),
                textValue(node, "Join Type"),
                textValue(node, "Strategy"),
                nullableDouble(node, "Startup Cost"),
                nullableDouble(node, "Total Cost"),
                nullableDouble(node, "Plan Rows"),
                nullableInteger(node, "Plan Width"),
                nullableDouble(node, "Actual Startup Time"),
                nullableDouble(node, "Actual Total Time"),
                nullableDouble(node, "Actual Rows"),
                nullableDouble(node, "Actual Loops"),
                List.copyOf(children)
        );
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private Double nullableDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || !value.isNumber()) {
            return null;
        }

        return value.doubleValue();
    }

    private Integer nullableInteger(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || !value.isIntegralNumber()
                || !value.canConvertToInt()) {
            return null;
        }

        return value.intValue();
    }
}
