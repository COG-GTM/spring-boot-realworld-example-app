package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  private User newUser() {
    return new User("user@example.com", "user", "123", "", "");
  }

  @Test
  void should_allow_author_to_write_article() {
    User author = newUser();
    Article article =
        new Article("title", "desc", "body", Collections.singletonList("java"), author.getId());

    assertTrue(AuthorizationService.canWriteArticle(author, article));
  }

  @Test
  void should_not_allow_non_author_to_write_article() {
    User author = newUser();
    User other = newUser();
    Article article =
        new Article("title", "desc", "body", Collections.singletonList("java"), author.getId());

    assertFalse(AuthorizationService.canWriteArticle(other, article));
  }

  @Test
  void article_author_can_write_comment_even_if_not_comment_author() {
    User articleAuthor = newUser();
    User commentAuthor = newUser();
    Article article =
        new Article(
            "title", "desc", "body", Collections.singletonList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, comment));
  }

  @Test
  void comment_author_can_write_comment_even_if_not_article_author() {
    User articleAuthor = newUser();
    User commentAuthor = newUser();
    Article article =
        new Article(
            "title", "desc", "body", Collections.singletonList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(commentAuthor, article, comment));
  }

  @Test
  void unrelated_user_cannot_write_comment() {
    User articleAuthor = newUser();
    User commentAuthor = newUser();
    User stranger = newUser();
    Article article =
        new Article(
            "title", "desc", "body", Collections.singletonList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());

    assertFalse(AuthorizationService.canWriteComment(stranger, article, comment));
  }
}
