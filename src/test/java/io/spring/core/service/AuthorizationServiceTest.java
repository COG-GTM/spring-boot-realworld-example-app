package io.spring.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  private final User author = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final User other = new User("john@john.john", "john", "123", "bio", "image");
  private final Article article =
      new Article("title", "desc", "body", Arrays.asList("java"), author.getId());

  @Test
  void should_allow_author_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article)).isTrue();
  }

  @Test
  void should_not_allow_other_user_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(other, article)).isFalse();
  }

  @Test
  void should_allow_article_author_to_write_any_comment() {
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment)).isTrue();
  }

  @Test
  void should_allow_comment_author_to_write_own_comment() {
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(other, article, comment)).isTrue();
  }

  @Test
  void should_not_allow_unrelated_user_to_write_comment() {
    User stranger = new User("stranger@x.com", "stranger", "123", "bio", "image");
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(stranger, article, comment)).isFalse();
  }

  @Test
  void should_be_instantiable() {
    assertThat(new AuthorizationService()).isNotNull();
  }
}
