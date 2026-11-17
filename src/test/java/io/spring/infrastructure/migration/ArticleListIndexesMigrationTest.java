package io.spring.infrastructure.migration;

import io.spring.infrastructure.DbTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ArticleListIndexesMigrationTest extends DbTestBase {
  @Autowired private DataSource dataSource;

  @Test
  public void should_create_article_list_indexes() throws Exception {
    Set<String> indexes = new HashSet<>();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery("select name from sqlite_master where type = 'index'")) {
      while (rs.next()) {
        indexes.add(rs.getString("name"));
      }
    }
    Assertions.assertTrue(indexes.contains("idx_article_tags_tag_id"));
    Assertions.assertTrue(indexes.contains("idx_article_favorites_user_id"));
    Assertions.assertTrue(indexes.contains("idx_follows_user_id"));
  }
}
