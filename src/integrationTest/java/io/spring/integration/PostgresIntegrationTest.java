package io.spring.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full application against a real PostgreSQL instance started by Testcontainers.
 *
 * <p>A single container is shared by every subclass (singleton container pattern) so the Spring
 * context is cached and the database is only started once per test run. Tables are truncated before
 * each test to keep tests independent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PostgresIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          DockerImageName.parse(System.getProperty("it.postgres.image", "postgres:15-alpine"))
              .asCompatibleSubstituteFor("postgres"));

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
  }

  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void truncateTables() {
    jdbcTemplate.execute(
        "truncate table article_favorites, article_tags, comments, follows, tags, articles, users");
  }
}
