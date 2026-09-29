package io.spring.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostgresSchemaIT extends PostgresIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  void should_run_against_postgresql() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
    }
  }

  @Test
  void should_apply_flyway_migrations() {
    List<String> versions =
        jdbcTemplate.queryForList(
            "select version from flyway_schema_history where success = true order by installed_rank",
            String.class);
    assertThat(versions).contains("1");

    List<String> tables =
        jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);
    assertThat(tables)
        .contains(
            "users",
            "articles",
            "article_favorites",
            "follows",
            "tags",
            "article_tags",
            "comments");
  }
}
