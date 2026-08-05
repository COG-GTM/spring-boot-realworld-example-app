package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User author;
  private User otherUser;
  private Article article;

  @BeforeEach
  public void setUp() {
    author = new User("author@test.com", "author", "123", "", "");
    otherUser = new User("other@test.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @Test
  public void should_allow_the_article_author_to_write_the_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article)).isTrue();
  }

  @Test
  public void should_not_allow_another_user_to_write_the_article() {
    assertThat(AuthorizationService.canWriteArticle(otherUser, article)).isFalse();
  }

  @Test
  public void should_not_allow_writing_an_article_without_an_author() {
    Article authorless = new Article("title", "desc", "body", Arrays.asList("java"), null);

    assertThat(AuthorizationService.canWriteArticle(author, authorless)).isFalse();
  }

  /**
   * Characterization of the current implementation: {@code user.getId().equals(...)} dereferences a
   * null id. It is not a desired guarantee -- adding defensive null handling to {@link
   * AuthorizationService} should come with an update to this test.
   */
  @Test
  public void should_throw_when_checking_article_permission_for_an_id_less_user() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> AuthorizationService.canWriteArticle(new User(), article));
  }

  @Test
  public void should_allow_the_comment_author_to_write_the_comment() {
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(otherUser, article, comment)).isTrue();
  }

  @Test
  public void should_allow_the_article_author_to_write_someone_else_comment() {
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  public void should_allow_the_author_of_both_the_article_and_the_comment() {
    Comment comment = new Comment("body", author.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  public void should_not_allow_an_unrelated_user_to_write_the_comment() {
    User unrelatedUser = new User("unrelated@test.com", "unrelated", "123", "", "");
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(unrelatedUser, article, comment)).isFalse();
  }

  @Test
  public void should_not_allow_writing_a_comment_without_an_article_author_and_comment_author() {
    Article authorless = new Article("title", "desc", "body", Arrays.asList("java"), null);
    Comment comment = new Comment("body", null, authorless.getId());

    assertThat(AuthorizationService.canWriteComment(otherUser, authorless, comment)).isFalse();
  }

  /** Characterization of the current implementation, see the article permission case above. */
  @Test
  public void should_throw_when_checking_comment_permission_for_an_id_less_user() {
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> AuthorizationService.canWriteComment(new User(), article, comment));
  }

  @Test
  public void should_be_instantiable() {
    assertThat(new AuthorizationService()).isNotNull();
  }
}
