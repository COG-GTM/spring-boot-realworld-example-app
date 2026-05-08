package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  public void canWriteArticle_should_be_true_when_user_is_author() {
    User user = new User("a@b.com", "alice", "secret", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());

    assertTrue(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void canWriteArticle_should_be_false_when_user_is_not_author() {
    User user = new User("a@b.com", "alice", "secret", "", "");
    User other = new User("b@b.com", "bob", "secret", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), other.getId());

    assertFalse(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  public void canWriteComment_should_be_true_when_user_is_article_owner() {
    User articleOwner = new User("a@b.com", "alice", "secret", "", "");
    User commentOwner = new User("b@b.com", "bob", "secret", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentOwner.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(articleOwner, article, comment));
  }

  @Test
  public void canWriteComment_should_be_true_when_user_is_comment_owner() {
    User articleOwner = new User("a@b.com", "alice", "secret", "", "");
    User commentOwner = new User("b@b.com", "bob", "secret", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentOwner.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(commentOwner, article, comment));
  }

  @Test
  public void canWriteComment_should_be_false_when_user_is_neither_owner() {
    User articleOwner = new User("a@b.com", "alice", "secret", "", "");
    User commentOwner = new User("b@b.com", "bob", "secret", "", "");
    User stranger = new User("c@b.com", "carol", "secret", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commentOwner.getId(), article.getId());

    assertFalse(AuthorizationService.canWriteComment(stranger, article, comment));
  }
}
