package io.spring.infrastructure.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FlywayMigrationTest {
  private static final String BASELINE_VERSION = "1";
  private static final String BASELINE_DESCRIPTION = "baseline of pre-migration schema";

  /**
   * Checksums of migrations that have been released. Applied migrations must never be edited:
   * Flyway refuses to start against a database whose history does not match. Add a new versioned
   * migration instead, then pin its checksum here.
   */
  private static final Map<String, Integer> RELEASED_CHECKSUMS =
      Map.of(
          "1", -420955279,
          "2", -1142151448);

  @TempDir Path tempDir;

  private String url;

  @BeforeEach
  public void setUp() {
    url = "jdbc:sqlite:" + tempDir.resolve("migration-test.db");
  }

  @Test
  public void should_migrate_empty_database_to_current_schema() throws Exception {
    flyway(false).migrate();

    assertEquals(expectedSchema("db/schema/current.sql"), actualSchema());
    assertTrue(
        Arrays.stream(flyway(false).info().all())
            .allMatch(info -> info.getState() == MigrationState.SUCCESS));
  }

  @Test
  public void should_build_baseline_schema_from_v1() throws Exception {
    flyway(false, BASELINE_VERSION).migrate();

    assertEquals(expectedSchema("db/schema/baseline-v1.sql"), actualSchema());
  }

  @Test
  public void should_keep_released_migrations_unchanged() {
    flyway(false).migrate();

    Map<String, Integer> applied = new LinkedHashMap<>();
    for (MigrationInfo info : flyway(false).info().applied()) {
      applied.put(info.getVersion().getVersion(), info.getChecksum());
    }
    assertEquals(RELEASED_CHECKSUMS, applied);
  }

  @Test
  public void should_upgrade_database_already_managed_at_v1() throws Exception {
    flyway(false, BASELINE_VERSION).migrate();
    insertSampleData();

    flyway(false).migrate();

    assertEquals(expectedSchema("db/schema/current.sql"), actualSchema());
    assertSampleDataPresent();
    MigrationInfo v1 = flyway(false).info().applied()[0];
    assertEquals(MigrationType.SQL, v1.getType());
    assertEquals(RELEASED_CHECKSUMS.get("1"), v1.getChecksum());
  }

  @Test
  public void should_refuse_unmanaged_database_unless_baselined() throws Exception {
    createLegacySchema();

    FlywayException error = assertThrows(FlywayException.class, () -> flyway(false).migrate());
    assertTrue(error.getMessage().contains("baseline"), error.getMessage());
  }

  @Test
  public void should_baseline_unmanaged_database_and_apply_later_migrations() throws Exception {
    createLegacySchema();
    insertSampleData();

    flyway(true).migrate();

    assertEquals(expectedSchema("db/schema/current.sql"), actualSchema());
    assertSampleDataPresent();
    MigrationInfo[] applied = flyway(false).info().applied();
    assertEquals(MigrationType.BASELINE, applied[0].getType());
    assertEquals(BASELINE_VERSION, applied[0].getVersion().getVersion());
    assertEquals(BASELINE_DESCRIPTION, applied[0].getDescription());
    assertTrue(
        Arrays.stream(applied).skip(1).allMatch(info -> info.getType() == MigrationType.SQL));
    flyway(false).validate();
  }

  @Test
  public void should_not_allow_clean() {
    flyway(false).migrate();

    assertThrows(FlywayException.class, () -> flyway(false).clean());
  }

  private Flyway flyway(boolean baselineOnMigrate) {
    return flyway(baselineOnMigrate, null);
  }

  private Flyway flyway(boolean baselineOnMigrate, String target) {
    return Flyway.configure()
        .dataSource(url, "", "")
        .locations("classpath:db/migration")
        .baselineVersion(BASELINE_VERSION)
        .baselineDescription(BASELINE_DESCRIPTION)
        .baselineOnMigrate(baselineOnMigrate)
        .cleanDisabled(true)
        .target(target == null ? "latest" : target)
        .load();
  }

  private void createLegacySchema() throws Exception {
    execute(readClasspath("db/migration/V1__create_tables.sql"));
  }

  private void insertSampleData() throws SQLException {
    execute(
        "insert into users (id, username, email, password, bio, image)"
            + " values ('u1', 'jake', 'jake@example.com', 'secret', '', '');"
            + "insert into articles (id, user_id, slug, title, description, body, created_at)"
            + " values ('a1', 'u1', 'hello', 'Hello', 'desc', 'body', CURRENT_TIMESTAMP);");
  }

  private void assertSampleDataPresent() throws SQLException {
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "select u.username from articles a join users u on u.id = a.user_id")) {
      assertTrue(rs.next());
      assertEquals("jake", rs.getString(1));
    }
  }

  private void execute(String script) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      for (String sql : splitStatements(script)) {
        statement.execute(sql);
      }
    }
  }

  private List<String> actualSchema() throws SQLException {
    List<String> statements = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "select sql from sqlite_master"
                    + " where sql is not null and name not like 'flyway_schema_history%'"
                    + " order by case type when 'table' then 0 else 1 end, name")) {
      while (rs.next()) {
        statements.add(normalize(rs.getString(1)));
      }
    }
    return statements;
  }

  private List<String> expectedSchema(String resource) throws IOException {
    return splitStatements(readClasspath(resource)).stream()
        .map(FlywayMigrationTest::normalize)
        .collect(Collectors.toList());
  }

  private static List<String> splitStatements(String script) {
    String withoutComments =
        Arrays.stream(script.split("\\R"))
            .filter(line -> !line.trim().startsWith("--"))
            .collect(Collectors.joining("\n"));
    return Arrays.stream(withoutComments.split(";"))
        .map(String::trim)
        .filter(sql -> !sql.isEmpty())
        .collect(Collectors.toList());
  }

  private static String normalize(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }

  private static String readClasspath(String resource) throws IOException {
    try (InputStream in =
        FlywayMigrationTest.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Missing classpath resource " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
