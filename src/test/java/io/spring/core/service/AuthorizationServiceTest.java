package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private final User author = new User("author@example.com", "author", "123", "", "");
  private final User other = new User("other@example.com", "other", "123", "", "");

  private Article articleOf(User user) {
    return new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @Test
  public void should_allow_author_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, articleOf(author))).isTrue();
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(other, articleOf(author))).isFalse();
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    Article article = articleOf(author);
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    Article article = articleOf(author);
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(other, article, comment)).isTrue();
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    Article article = articleOf(author);
    Comment comment = new Comment("body", author.getId(), article.getId());
    User stranger = new User("stranger@example.com", "stranger", "123", "", "");

    assertThat(AuthorizationService.canWriteComment(stranger, article, comment)).isFalse();
  }
}
