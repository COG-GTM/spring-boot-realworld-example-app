package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  public void should_allow_author_to_write_article() {
    User user = new User("test@test.com", "user", "pass", "", "");
    Article article = new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    assertTrue(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void should_deny_non_author_to_write_article() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User other = new User("other@test.com", "other", "pass", "", "");
    Article article = new Article("Title", "Desc", "Body", Arrays.asList(), author.getId());
    assertFalse(AuthorizationService.canWriteArticle(other, article));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, comment));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(commenter, article, comment));
  }

  @Test
  public void should_deny_random_user_to_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    User random = new User("random@test.com", "random", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertFalse(AuthorizationService.canWriteComment(random, article, comment));
  }
}
