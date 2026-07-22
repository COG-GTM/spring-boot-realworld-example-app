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
  public void should_allow_author_to_write_article() {
    User author = new User("author@test.com", "author", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }

  @Test
  public void should_forbid_non_author_to_write_article() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User other = new User("other@test.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(author, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(commenter, article, comment), is(true));
  }

  @Test
  public void should_forbid_unrelated_user_to_write_comment() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    User other = new User("other@test.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(other, article, comment), is(false));
  }
}
