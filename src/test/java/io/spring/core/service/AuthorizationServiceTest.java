package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User newUser(String username) {
    return new User(username + "@test.com", username, "123", "bio", "");
  }

  private Article newArticle(String userId) {
    return new Article("a title", "desc", "body", Arrays.asList("java"), userId);
  }

  @Test
  public void should_allow_author_to_write_article() {
    User author = newUser("author");
    Article article = newArticle(author.getId());

    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    User author = newUser("author");
    User other = newUser("other");
    Article article = newArticle(author.getId());

    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_not_allow_write_article_when_article_has_no_author() {
    User user = newUser("user");
    Article article = newArticle(null);

    assertThat(AuthorizationService.canWriteArticle(user, article), is(false));
  }

  @Test
  public void should_throw_when_writing_article_with_user_without_id() {
    Article article = newArticle("123");

    assertThrows(
        NullPointerException.class,
        () -> AuthorizationService.canWriteArticle(new User(), article));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor = newUser("articleAuthor");
    User commentAuthor = newUser("commentAuthor");
    Article article = newArticle(articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(commentAuthor, article, comment), is(true));
  }

  @Test
  public void should_allow_article_author_to_write_others_comment() {
    User articleAuthor = newUser("articleAuthor");
    User commentAuthor = newUser("commentAuthor");
    Article article = newArticle(articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(articleAuthor, article, comment), is(true));
  }

  @Test
  public void should_allow_author_of_both_article_and_comment_to_write_comment() {
    User author = newUser("author");
    Article article = newArticle(author.getId());
    Comment comment = new Comment("comment body", author.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment), is(true));
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    User articleAuthor = newUser("articleAuthor");
    User commentAuthor = newUser("commentAuthor");
    User other = newUser("other");
    Article article = newArticle(articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(other, article, comment), is(false));
  }

  @Test
  public void should_not_allow_write_comment_when_article_and_comment_have_no_author() {
    User user = newUser("user");
    Article article = newArticle(null);
    Comment comment = new Comment("comment body", null, article.getId());

    assertThat(AuthorizationService.canWriteComment(user, article, comment), is(false));
  }

  @Test
  public void should_throw_when_writing_comment_with_user_without_id() {
    Article article = newArticle("123");
    Comment comment = new Comment("comment body", "456", article.getId());

    assertThrows(
        NullPointerException.class,
        () -> AuthorizationService.canWriteComment(new User(), article, comment));
  }

  @Test
  public void should_not_allow_write_article_when_ids_differ_only_by_case() {
    User user = newUser("user");
    Article article = newArticle(user.getId().toUpperCase());

    assertThat(AuthorizationService.canWriteArticle(user, article), is(false));
  }

  @Test
  public void should_handle_article_without_tags() {
    User author = newUser("author");
    Article article =
        new Article("a title", "desc", "body", Collections.emptyList(), author.getId());

    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }
}
