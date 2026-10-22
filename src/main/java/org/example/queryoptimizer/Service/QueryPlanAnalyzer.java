package org.example.queryoptimizer.Service;



import org.example.queryoptimizer.Dto.QueryPlanDto;
import org.example.queryoptimizer.Dto.QueryPlanFindingDto;
import org.example.queryoptimizer.Dto.QueryPlanNodeDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryPlanAnalyzer {

    public List<QueryPlanFindingDto> analyze(QueryPlanDto plan) {
        List<QueryPlanFindingDto> findings = new ArrayList<>();

        if (plan == null || plan.rootPlan() == null) {
            return findings;
        }

        inspectNode(plan.rootPlan(), findings);

        return findings;
    }

    private void inspectNode(
            QueryPlanNodeDto node,
            List<QueryPlanFindingDto> findings
    ) {
        if (node == null) {
            return;
        }

        String nodeType = node.nodeType();
        String relationName = node.relationName();

        if ("Seq Scan".equals(nodeType)) {
            findings.add(new QueryPlanFindingDto(
                    "SCAN",
                    "INFO",
                    nodeType,
                    relationName,
                    "PostgreSQL selected a sequential scan"
                            + tableSuffix(relationName)
                            + ". Check whether scanning the table is appropriate "
                            + "for the query and table size."
            ));
        }

        if ("Index Scan".equals(nodeType)
                || "Index Only Scan".equals(nodeType)
                || "Bitmap Index Scan".equals(nodeType)) {
            findings.add(new QueryPlanFindingDto(
                    "INDEX_USAGE",
                    "INFO",
                    nodeType,
                    relationName,
                    "PostgreSQL selected an index-based scan"
                            + tableSuffix(relationName) + "."
            ));
        }

        if ("Hash Join".equals(nodeType)
                || "Merge Join".equals(nodeType)
                || "Nested Loop".equals(nodeType)) {
            findings.add(new QueryPlanFindingDto(
                    "JOIN_ALGORITHM",
                    "INFO",
                    nodeType,
                    null,
                    "PostgreSQL selected the "
                            + nodeType
                            + " join algorithm. Its efficiency depends "
                            + "on row counts, available indexes, and data distribution."
            ));
        }

        if (node.children() != null) {
            for (QueryPlanNodeDto child : node.children()) {
                inspectNode(child, findings);
            }
        }
    }

    private String tableSuffix(String relationName) {
        return relationName == null
                ? ""
                : " on table " + relationName;
    }
}

