package org.example.queryoptimizer.Config;

import org.example.queryoptimizer.Dto.Documentation.DocumentationSource;

import java.util.List;

public class PostgresDocumentationSources {

    private PostgresDocumentationSources() {}

    public static List<DocumentationSource> all() {
        return List.of(
                new DocumentationSource(
                        "Using EXPLAIN",
                        "https://www.postgresql.org/docs/current/using-explain.html",
                        "EXECUTION_PLANS"
                ),
                new DocumentationSource(
                        "Planner Configuration",
                        "https://www.postgresql.org/docs/current/runtime-config-query.html",
                        "QUERY_PLANNER"
                ),
                new DocumentationSource(
                        "Indexes",
                        "https://www.postgresql.org/docs/current/indexes.html",
                        "INDEXES"
                ),
                new DocumentationSource(
                        "Examining Index Usage",
                        "https://www.postgresql.org/docs/current/indexes-examine.html",
                        "INDEX_USAGE"
                ),
                new DocumentationSource(
                        "Planner Statistics",
                        "https://www.postgresql.org/docs/current/planner-stats.html",
                        "STATISTICS"
                )
        );
    }
}
