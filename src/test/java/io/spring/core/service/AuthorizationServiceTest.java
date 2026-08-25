package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private final User author = new User("author@example.com", "author", "123", "", "");
  private final User other = new User("other@example.com", "other", "123", "", "");
  private final Article article =
      new Article("title", "desc", "body", Collections.emptyList(), author.getId());

  @Test
  public void should_allow_only_the_author_to_write_the_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article)).isTrue();
    assertThat(AuthorizationService.canWriteArticle(other, article)).isFalse();
  }

  @Test
  public void should_allow_article_author_and_comment_author_to_write_the_comment() {
    Comment comment = new Comment("comment body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
    assertThat(AuthorizationService.canWriteComment(other, article, comment)).isTrue();
  }

  @Test
  public void should_reject_unrelated_user_to_write_the_comment() {
    Comment comment = new Comment("comment body", author.getId(), article.getId());
    User stranger = new User("stranger@example.com", "stranger", "123", "", "");

    assertThat(AuthorizationService.canWriteComment(stranger, article, comment)).isFalse();
  }
}
