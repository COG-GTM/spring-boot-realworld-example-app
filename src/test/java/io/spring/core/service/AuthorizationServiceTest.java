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
  public void article_owner_should_be_authorized_to_write_article() {
    User user = new User("e@e.com", "user", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());

    assertThat(AuthorizationService.canWriteArticle(user, article), is(true));
  }

  @Test
  public void non_owner_should_not_be_authorized_to_write_article() {
    User owner = new User("owner@e.com", "owner", "pass", "", "");
    User other = new User("other@e.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());

    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void article_owner_should_be_authorized_to_write_comment() {
    User articleOwner = new User("owner@e.com", "owner", "pass", "", "");
    User commenter = new User("commenter@e.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(articleOwner, article, comment), is(true));
  }

  @Test
  public void comment_owner_should_be_authorized_to_write_comment() {
    User articleOwner = new User("owner@e.com", "owner", "pass", "", "");
    User commenter = new User("commenter@e.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(commenter, article, comment), is(true));
  }

  @Test
  public void non_owner_should_not_be_authorized_to_write_comment() {
    User articleOwner = new User("owner@e.com", "owner", "pass", "", "");
    User commenter = new User("commenter@e.com", "commenter", "pass", "", "");
    User stranger = new User("stranger@e.com", "stranger", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(stranger, article, comment), is(false));
  }
}
