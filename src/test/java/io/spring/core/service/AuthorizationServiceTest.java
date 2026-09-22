package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private final User author = new User("author@test.com", "author", "123", "", "");
  private final User other = new User("other@test.com", "other", "123", "", "");
  private final Article article =
      new Article("title", "desc", "body", Arrays.asList("java"), author.getId());

  @Test
  public void should_allow_author_to_write_article() {
    assertTrue(AuthorizationService.canWriteArticle(author, article));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertFalse(AuthorizationService.canWriteArticle(other, article));
  }

  @Test
  public void should_allow_article_author_to_delete_any_comment() {
    Comment comment = new Comment("content", other.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(author, article, comment));
  }

  @Test
  public void should_allow_comment_author_to_write_own_comment() {
    Comment comment = new Comment("content", other.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(other, article, comment));
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    User unrelated = new User("unrelated@test.com", "unrelated", "123", "", "");
    Comment comment = new Comment("content", other.getId(), article.getId());
    assertFalse(AuthorizationService.canWriteComment(unrelated, article, comment));
  }
}
