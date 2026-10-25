package org.example.queryoptimizer.knowledge.Config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Datasource for the knowledge store (pgvector).
 * Marked @Primary so Spring AI's pgvector auto-configuration and JPA pick this one by default.
 */
@Configuration
public class VectorDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.vector.datasource")
    public DataSource vectorDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public JdbcTemplate vectorJdbcTemplate(DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
