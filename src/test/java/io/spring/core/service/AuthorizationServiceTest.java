package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private final User author = new User("author@example.com", "author", "123", "bio", "image");
  private final User other = new User("other@example.com", "other", "123", "bio", "image");

  @Test
  public void should_allow_author_to_write_article() {
    Article article = articleOf(author);

    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    Article article = articleOf(author);

    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_others_comment() {
    Article article = articleOf(author);
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(author, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_own_comment() {
    Article article = articleOf(author);
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(other, article, comment), is(true));
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    User unrelated = new User("unrelated@example.com", "unrelated", "123", "bio", "image");
    Article article = articleOf(author);
    Comment comment = new Comment("body", other.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(unrelated, article, comment), is(false));
  }

  private static Article articleOf(User user) {
    return new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }
}
