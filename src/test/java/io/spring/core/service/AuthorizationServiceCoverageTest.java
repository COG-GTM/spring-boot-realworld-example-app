package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceCoverageTest {

  @Test
  public void should_allow_author_to_write_article() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag"), user.getId());

    assertTrue(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void should_deny_non_author_to_write_article() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    User anotherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag"), anotherUser.getId());

    assertFalse(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag"), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, comment));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag"), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(commentAuthor, article, comment));
  }

  @Test
  public void should_deny_unrelated_user_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    User randomUser = new User("random@test.com", "random", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("tag"), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());

    assertFalse(AuthorizationService.canWriteComment(randomUser, article, comment));
  }
}
