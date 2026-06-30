package io.spring.infrastructure;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual Flyway configuration. Spring Boot 3.3+ ships Flyway 10+, which dropped SQLite support, so
 * {@code FlywayAutoConfiguration} is excluded and Flyway 9.x (the last line supporting SQLite) is
 * wired up here against the application {@link DataSource}.
 */
@Configuration
public class FlywayConfig {

  @Bean(initMethod = "migrate")
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).load();
  }
}
