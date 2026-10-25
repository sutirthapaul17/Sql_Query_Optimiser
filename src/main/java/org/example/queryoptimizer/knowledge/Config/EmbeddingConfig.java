package org.example.queryoptimizer.knowledge.Config;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires the embedding model to the pgvector store, bound explicitly to the knowledge datasource.
 *
 * The EmbeddingModel itself comes from the Spring AI starter you already use
 * (OpenAI, Ollama, ...) via spring.ai.* properties, so it is only injected here.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public VectorStore vectorStore(@Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
                                   EmbeddingModel embeddingModel,
                                   @Value("${app.vector.table-name:vector_store}") String tableName,
                                   @Value("${app.vector.dimensions:0}") int dimensions,
                                   @Value("${app.vector.initialize-schema:true}") boolean initializeSchema) {

        PgVectorStore.PgVectorStoreBuilder builder = PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(initializeSchema);

        if (dimensions > 0) {
            builder.dimensions(dimensions);   // must match the embedding model's output size
        }
        return builder.build();
    }
}
