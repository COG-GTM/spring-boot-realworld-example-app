package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private final User author = new User("author@test.com", "author", "123", "", "");
  private final User other = new User("other@test.com", "other", "123", "", "");
  private final Article article =
      new Article("title", "desc", "body", Arrays.asList("java"), author.getId());

  @Test
  public void should_allow_author_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(author, article), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment_of_other_user() {
    Comment comment = new Comment("comment", other.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(author, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_own_comment() {
    Comment comment = new Comment("comment", other.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(other, article, comment), is(true));
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    User unrelated = new User("unrelated@test.com", "unrelated", "123", "", "");
    Comment comment = new Comment("comment", other.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(unrelated, article, comment), is(false));
  }
}
