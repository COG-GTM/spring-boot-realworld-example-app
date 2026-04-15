package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User articleOwner;
  private User commentOwner;
  private User otherUser;
  private Article article;
  private Comment comment;

  @BeforeEach
  void setUp() {
    articleOwner = new User("owner@example.com", "owner", "pass", "", "");
    commentOwner = new User("commenter@example.com", "commenter", "pass", "", "");
    otherUser = new User("other@example.com", "other", "pass", "", "");
    article = new Article("Title", "desc", "body", Collections.emptyList(), articleOwner.getId());
    comment = new Comment("comment body", commentOwner.getId(), article.getId());
  }

  @Test
  void article_owner_can_write_article() {
    assertTrue(AuthorizationService.canWriteArticle(articleOwner, article));
  }

  @Test
  void non_owner_cannot_write_article() {
    assertFalse(AuthorizationService.canWriteArticle(otherUser, article));
  }

  @Test
  void article_owner_can_write_comment_on_own_article() {
    assertTrue(AuthorizationService.canWriteComment(articleOwner, article, comment));
  }

  @Test
  void comment_owner_can_write_own_comment() {
    assertTrue(AuthorizationService.canWriteComment(commentOwner, article, comment));
  }

  @Test
  void other_user_cannot_write_comment() {
    assertFalse(AuthorizationService.canWriteComment(otherUser, article, comment));
  }

  @Test
  void comment_owner_can_delete_own_comment_on_others_article() {
    Article othersArticle =
        new Article("Title", "desc", "body", Collections.emptyList(), otherUser.getId());
    Comment myComment = new Comment("body", commentOwner.getId(), othersArticle.getId());
    assertTrue(AuthorizationService.canWriteComment(commentOwner, othersArticle, myComment));
  }
}
