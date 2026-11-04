package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    comment = new Comment("content", commenter.getId(), article.getId());
  }

  @Test
  public void should_allow_author_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(stranger, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(author, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(commenter, article, comment), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_comment() {
    assertThat(AuthorizationService.canWriteComment(stranger, article, comment), is(false));
  }
}
