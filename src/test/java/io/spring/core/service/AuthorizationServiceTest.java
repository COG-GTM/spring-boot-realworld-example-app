package io.spring.core.service;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User author;
  private User other;
  private Article article;

  @BeforeEach
  public void setUp() {
    author = new User("author@example.com", "author", "123", "", "");
    other = new User("other@example.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @Test
  public void author_can_write_article() {
    Assertions.assertTrue(AuthorizationService.canWriteArticle(author, article));
  }

  @Test
  public void other_user_cannot_write_article() {
    Assertions.assertFalse(AuthorizationService.canWriteArticle(other, article));
  }

  @Test
  public void article_author_can_write_any_comment() {
    Comment comment = new Comment("content", other.getId(), article.getId());
    Assertions.assertTrue(AuthorizationService.canWriteComment(author, article, comment));
  }

  @Test
  public void comment_author_can_write_own_comment() {
    Comment comment = new Comment("content", other.getId(), article.getId());
    Assertions.assertTrue(AuthorizationService.canWriteComment(other, article, comment));
  }

  @Test
  public void third_party_cannot_write_comment() {
    User third = new User("third@example.com", "third", "123", "", "");
    Comment comment = new Comment("content", other.getId(), article.getId());
    Assertions.assertFalse(AuthorizationService.canWriteComment(third, article, comment));
  }
}
