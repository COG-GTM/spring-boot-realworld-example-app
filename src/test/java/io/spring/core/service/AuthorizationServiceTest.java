package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.TestHelper;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
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
    author = TestHelper.userFixture("author");
    commenter = TestHelper.userFixture("commenter");
    stranger = TestHelper.userFixture("stranger");
    article = TestHelper.articleFixture("article", author);
    comment = TestHelper.commentFixture("comment", article, commenter);
  }

  @Test
  public void should_allow_author_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article)).isTrue();
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(stranger, article)).isFalse();
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(commenter, article, comment)).isTrue();
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(stranger, article, comment)).isFalse();
  }
}
