package org.example.queryoptimizer.Config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean(name = "tpchDataSource")
    @ConfigurationProperties(prefix = "app.datasource.tpch")
    public DataSource tpchDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "ragDataSource")
    @Primary
    @ConfigurationProperties(prefix = "app.datasource.rag")
    public DataSource ragDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "tpchJdbcTemplate")
    public JdbcTemplate tpchJdbcTemplate(
            @Qualifier("tpchDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
