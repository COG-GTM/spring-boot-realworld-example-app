package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User articleAuthor;
  private User otherUser;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleAuthor = new User("author@email.com", "author", "password", "", "");
    otherUser = new User("other@email.com", "other", "password", "", "");
    article = new Article("Test Article", "description", "body", Arrays.asList("tag"), articleAuthor.getId());
  }

  @Test
  public void should_allow_author_to_write_article() {
    assertTrue(AuthorizationService.canWriteArticle(articleAuthor, article));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertFalse(AuthorizationService.canWriteArticle(otherUser, article));
  }

  @Test
  public void should_allow_article_author_to_write_any_comment_on_their_article() {
    Comment commentByOther = new Comment("comment body", otherUser.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, commentByOther));
  }

  @Test
  public void should_allow_comment_author_to_write_their_own_comment() {
    Comment commentByOther = new Comment("comment body", otherUser.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(otherUser, article, commentByOther));
  }

  @Test
  public void should_not_allow_random_user_to_write_comment_on_others_article() {
    User randomUser = new User("random@email.com", "random", "password", "", "");
    Comment commentByOther = new Comment("comment body", otherUser.getId(), article.getId());

    assertFalse(AuthorizationService.canWriteComment(randomUser, article, commentByOther));
  }

  @Test
  public void should_allow_author_to_write_their_own_comment_on_their_article() {
    Comment commentByAuthor = new Comment("comment body", articleAuthor.getId(), article.getId());

    assertTrue(AuthorizationService.canWriteComment(articleAuthor, article, commentByAuthor));
  }
}
