package io.spring.core.service;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  private User articleOwner;
  private User otherUser;
  private Article article;

  @BeforeEach
  void setUp() {
    articleOwner = new User("owner@test.com", "owner", "pass", "", "");
    otherUser = new User("other@test.com", "other", "pass", "", "");
    article = new Article("Test Title", "desc", "body", emptyList(), articleOwner.getId());
  }

  @Test
  void owner_can_write_article() {
    assertTrue(AuthorizationService.canWriteArticle(articleOwner, article));
  }

  @Test
  void non_owner_cannot_write_article() {
    assertFalse(AuthorizationService.canWriteArticle(otherUser, article));
  }

  @Test
  void article_owner_can_delete_any_comment() {
    Comment comment = new Comment("comment body", otherUser.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(articleOwner, article, comment));
  }

  @Test
  void comment_author_can_delete_own_comment() {
    Comment comment = new Comment("comment body", otherUser.getId(), article.getId());
    assertTrue(AuthorizationService.canWriteComment(otherUser, article, comment));
  }

  @Test
  void unrelated_user_cannot_delete_comment() {
    User unrelatedUser = new User("unrelated@test.com", "unrelated", "pass", "", "");
    Comment comment = new Comment("comment body", otherUser.getId(), article.getId());
    assertFalse(AuthorizationService.canWriteComment(unrelatedUser, article, comment));
  }
}
