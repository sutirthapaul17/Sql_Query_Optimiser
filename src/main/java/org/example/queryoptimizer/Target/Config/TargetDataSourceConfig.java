package org.example.queryoptimizer.Target.Config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Datasource for the database being optimized (TPC-H now, other PostgreSQL databases later).
 * Read-only by default. Properties under app.target.datasource override these defaults.
 */
@Configuration
public class TargetDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.target.datasource")
    public DataSource targetDataSource() {
        HikariDataSource ds = DataSourceBuilder.create().type(HikariDataSource.class).build();
        ds.setReadOnly(true);
        ds.setPoolName("target-pool");
        return ds;
    }

    @Bean
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
