package io.spring.infrastructure;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class QueryIndexTest extends DbTestBase {
  @Autowired private DataSource dataSource;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  private void assertUsesIndex(String sql, String index, Object... args) {
    List<String> plan =
        jdbcTemplate.query(
            "EXPLAIN QUERY PLAN " + sql, (rs, rowNum) -> rs.getString("detail"), args);
    Assertions.assertTrue(
        plan.stream().anyMatch(detail -> detail.contains(index)),
        () -> "expected " + index + " in plan " + plan);
  }

  @Test
  public void should_list_recent_articles_using_created_at_index() {
    assertUsesIndex(
        "select A.id from articles A order by A.created_at desc limit 20",
        "idx_articles_created_at");
  }

  @Test
  public void should_read_feed_using_follows_and_author_indexes() {
    String sql =
        "select A.id from articles A where A.user_id in "
            + "(select F.follow_id from follows F where F.user_id = ?) "
            + "order by A.created_at desc limit 20";
    assertUsesIndex(sql, "idx_follows_user_id_follow_id", "u1");
    assertUsesIndex(sql, "idx_articles_user_id_created_at", "u1");
  }

  @Test
  public void should_filter_articles_by_tag_using_tag_indexes() {
    String sql =
        "select AT.article_id from article_tags AT join tags T on T.id = AT.tag_id"
            + " where T.name = ?";
    assertUsesIndex(sql, "idx_tags_name", "java");
    assertUsesIndex(sql, "idx_article_tags_tag_id", "java");
  }

  @Test
  public void should_load_article_tags_using_article_id_index() {
    assertUsesIndex(
        "select tag_id from article_tags where article_id = ?",
        "idx_article_tags_article_id",
        "a1");
  }

  @Test
  public void should_filter_favorites_by_user_using_index() {
    assertUsesIndex(
        "select article_id from article_favorites where user_id = ?",
        "idx_article_favorites_user_id",
        "u1");
  }

  @Test
  public void should_page_comments_of_article_using_index() {
    assertUsesIndex(
        "select id from comments where article_id = ? order by created_at desc limit 21",
        "idx_comments_article_id_created_at",
        "a1");
  }
}
