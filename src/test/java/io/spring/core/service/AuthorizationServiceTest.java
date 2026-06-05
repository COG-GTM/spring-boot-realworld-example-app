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
  public void should_allow_article_owner_to_write_article() {
    User owner = new User("owner@test.com", "owner", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
    assertThat(AuthorizationService.canWriteArticle(owner, article), is(true));
  }

  @Test
  public void should_deny_non_owner_to_write_article() {
    User owner = new User("owner@test.com", "owner", "pass", "", "");
    User other = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_allow_article_owner_to_write_comment() {
    User articleOwner = new User("owner@test.com", "owner", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(articleOwner, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleOwner = new User("owner@test.com", "owner", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(commentAuthor, article, comment), is(true));
  }

  @Test
  public void should_deny_unrelated_user_to_write_comment() {
    User articleOwner = new User("owner@test.com", "owner", "pass", "", "");
    User commentAuthor = new User("commenter@test.com", "commenter", "pass", "", "");
    User unrelated = new User("unrelated@test.com", "unrelated", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(unrelated, article, comment), is(false));
  }
}
