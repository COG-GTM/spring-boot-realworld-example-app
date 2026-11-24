package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {
  private User author;
  private User commenter;
  private User stranger;
  private Article article;
  private Comment comment;

  @BeforeEach
  public void setUp() {
    author = new User("author@test.com", "author", "123", "", "");
    commenter = new User("commenter@test.com", "commenter", "123", "", "");
    stranger = new User("stranger@test.com", "stranger", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    comment = new Comment("comment", commenter.getId(), article.getId());
  }

  @Test
  public void author_can_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article)).isTrue();
  }

  @Test
  public void other_user_cannot_write_article() {
    assertThat(AuthorizationService.canWriteArticle(stranger, article)).isFalse();
    assertThat(AuthorizationService.canWriteArticle(commenter, article)).isFalse();
  }

  @Test
  public void comment_owner_can_write_comment() {
    assertThat(AuthorizationService.canWriteComment(commenter, article, comment)).isTrue();
  }

  @Test
  public void article_author_can_write_comment_on_own_article() {
    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  public void other_user_cannot_write_comment() {
    assertThat(AuthorizationService.canWriteComment(stranger, article, comment)).isFalse();
  }
}
