package io.spring.infrastructure;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Truncates every application table before each test class so that test classes sharing the same
 * PostgreSQL container do not see each other's committed data.
 */
public class DatabaseCleanupListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    ApplicationContext context = testContext.getApplicationContext();
    DataSource dataSource = context.getBeanProvider(DataSource.class).getIfUnique();
    if (dataSource == null) {
      return;
    }
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      List<String> tables = applicationTables(statement);
      if (tables.isEmpty()) {
        return;
      }
      statement.execute("truncate table " + String.join(", ", tables) + " cascade");
    }
  }

  private List<String> applicationTables(Statement statement) throws Exception {
    List<String> tables = new ArrayList<>();
    try (ResultSet resultSet =
        statement.executeQuery(
            "select table_name from information_schema.tables"
                + " where table_schema = current_schema()"
                + " and table_type = 'BASE TABLE'"
                + " and table_name <> 'flyway_schema_history'")) {
      while (resultSet.next()) {
        tables.add(resultSet.getString(1));
      }
    }
    return tables;
  }
}
