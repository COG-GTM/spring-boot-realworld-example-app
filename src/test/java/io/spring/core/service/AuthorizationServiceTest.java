package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  void canWriteArticle_should_return_true_when_user_is_author() {
    User user = new User("e@t.com", "u", "p", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    assertTrue(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  void canWriteArticle_should_return_false_when_user_is_not_author() {
    User user = new User("e@t.com", "u", "p", "", "");
    User other = new User("o@t.com", "o", "p", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), other.getId());
    assertFalse(AuthorizationService.canWriteArticle(user, article));
  }

  @Test
  void canWriteComment_should_return_true_when_user_is_article_owner() {
    User user = new User("e@t.com", "u", "p", "", "");
    User commenter = new User("c@t.com", "c", "p", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(user, article, comment));
  }

  @Test
  void canWriteComment_should_return_true_when_user_is_comment_author() {
    User articleOwner = new User("o@t.com", "o", "p", "", "");
    User commenter = new User("c@t.com", "c", "p", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(commenter, article, comment));
  }

  @Test
  void canWriteComment_should_return_false_when_user_is_neither_owner_nor_author() {
    User articleOwner = new User("o@t.com", "o", "p", "", "");
    User commenter = new User("c@t.com", "c", "p", "", "");
    User stranger = new User("s@t.com", "s", "p", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    assertFalse(AuthorizationService.canWriteComment(stranger, article, comment));
  }
}
