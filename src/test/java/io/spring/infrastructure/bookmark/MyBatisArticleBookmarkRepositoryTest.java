package io.spring.infrastructure.bookmark;

import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.mybatis.mapper.ArticleBookmarkMapper;
import io.spring.infrastructure.repository.MyBatisArticleBookmarkRepository;
import java.util.Optional;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Covers docs/specs/article-bookmarks.md AC-1, AC-2, AC-3, AC-11 at the persistence layer. */
@Import({MyBatisArticleBookmarkRepository.class})
public class MyBatisArticleBookmarkRepositoryTest extends DbTestBase {
  @Autowired private ArticleBookmarkRepository articleBookmarkRepository;

  @Autowired private ArticleBookmarkMapper articleBookmarkMapper;

  @Autowired private DataSource dataSource;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  private int countRows(String articleId, String userId) {
    return jdbcTemplate.queryForObject(
        "select count(1) from article_bookmarks where article_id = ? and user_id = ?",
        Integer.class,
        articleId,
        userId);
  }

  // AC-1
  @Test
  public void ac1_should_save_and_fetch_bookmark_success() {
    DateTime createdAt = new DateTime().withMillisOfSecond(0);
    articleBookmarkRepository.save(new ArticleBookmark("123", "456", createdAt));

    Optional<ArticleBookmark> fetched = articleBookmarkRepository.find("123", "456");
    Assertions.assertTrue(fetched.isPresent());
    Assertions.assertEquals("123", fetched.get().getArticleId());
    Assertions.assertEquals("456", fetched.get().getUserId());
    Assertions.assertEquals(createdAt.getMillis(), fetched.get().getCreatedAt().getMillis());
    Assertions.assertNotNull(articleBookmarkMapper.find("123", "456"));
  }

  // AC-2
  @Test
  public void ac2_should_not_duplicate_bookmark_on_repeated_save() {
    articleBookmarkRepository.save(new ArticleBookmark("123", "456"));
    articleBookmarkRepository.save(new ArticleBookmark("123", "456"));
    Assertions.assertEquals(1, countRows("123", "456"));
  }

  // AC-3
  @Test
  public void ac3_should_remove_bookmark_success() {
    ArticleBookmark bookmark = new ArticleBookmark("123", "456");
    articleBookmarkRepository.save(bookmark);
    articleBookmarkRepository.remove(bookmark);
    Assertions.assertFalse(articleBookmarkRepository.find("123", "456").isPresent());
    Assertions.assertEquals(0, countRows("123", "456"));
  }

  // AC-11
  @Test
  public void ac11_should_reject_duplicate_insert_at_database_level() {
    articleBookmarkMapper.insert(new ArticleBookmark("123", "456"));
    Assertions.assertThrows(
        DataAccessException.class,
        () -> articleBookmarkMapper.insert(new ArticleBookmark("123", "456")));
  }

  // AC-11
  @Test
  public void ac11_migration_should_create_table_with_unique_user_article_constraint() {
    String ddl =
        jdbcTemplate.queryForObject(
            "select sql from sqlite_master where type = 'table' and name = 'article_bookmarks'",
            String.class);
    Assertions.assertNotNull(ddl);
    Assertions.assertTrue(ddl.toLowerCase().contains("unique (user_id, article_id)"), ddl);
    Integer migrations =
        jdbcTemplate.queryForObject(
            "select count(1) from flyway_schema_history"
                + " where script = 'V2__create_article_bookmarks.sql' and success = 1",
            Integer.class);
    Assertions.assertEquals(1, migrations);
  }
}
