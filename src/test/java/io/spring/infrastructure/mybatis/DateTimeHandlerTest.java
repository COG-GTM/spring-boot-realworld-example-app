package io.spring.infrastructure.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.time.Instant;
import java.util.Arrays;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Pins the on-disk timestamp representation written by {@link DateTimeHandler}. */
@Import({MyBatisArticleRepository.class, MyBatisUserRepository.class})
public class DateTimeHandlerTest extends DbTestBase {

  @Autowired private SqlSessionFactory sqlSessionFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  /**
   * Unlike Joda's {@code DateTime}, {@code Instant} has a built-in MyBatis handler that would
   * silently write a different on-disk value. Our handler must win the registry lookup.
   */
  @Test
  public void should_register_handler_for_instant() {
    assertInstanceOf(
        DateTimeHandler.class,
        sqlSessionFactory
            .getConfiguration()
            .getTypeHandlerRegistry()
            .getTypeHandler(Instant.class));
  }

  @Test
  public void should_store_timestamps_as_epoch_millis() {
    User user = new User("handler@test.com", "handler", "123", "", "");
    userRepository.save(user);
    Instant createdAt = Instant.ofEpochMilli(1609502400123L);
    Article article =
        new Article("stored at", "desc", "body", Arrays.asList("java"), user.getId(), createdAt);
    articleRepository.save(article);

    jdbcTemplate.query(
        "select created_at, updated_at from articles where id = ?",
        rs -> {
          assertEquals(1609502400123L, rs.getLong("created_at"));
          assertEquals(1609502400123L, rs.getLong("updated_at"));
        },
        article.getId());

    assertEquals(createdAt, articleRepository.findById(article.getId()).get().getCreatedAt());
  }
}
