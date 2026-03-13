package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  public void should_return_true_when_user_is_article_author() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    assertThat(AuthorizationService.canWriteArticle(user, article), is(true));
  }

  @Test
  public void should_return_false_when_user_is_not_article_author() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    User other = new User("other@example.com", "other", "password", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), other.getId());
    assertThat(AuthorizationService.canWriteArticle(user, article), is(false));
  }

  @Test
  public void should_allow_comment_write_when_user_is_article_author() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    User commenter = new User("commenter@example.com", "commenter", "password", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(user, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_write_when_user_is_comment_author() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    User articleAuthor =
        new User("author@example.com", "author", "password", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", user.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(user, article, comment), is(true));
  }

  @Test
  public void should_deny_comment_write_when_user_is_neither_article_nor_comment_author() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    User articleAuthor =
        new User("author@example.com", "author", "password", "bio", "image");
    User commenter = new User("commenter@example.com", "commenter", "password", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(user, article, comment), is(false));
  }
}
