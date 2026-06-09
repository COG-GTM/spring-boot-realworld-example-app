package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  @Test
  public void should_allow_article_author_to_write_article() {
    User user = new User("a@b.com", "user1", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList(), user.getId());
    assertThat(AuthorizationService.canWriteArticle(user, article), is(true));
  }

  @Test
  public void should_deny_non_author_to_write_article() {
    User author = new User("a@b.com", "author", "pass", "", "");
    User other = new User("c@d.com", "other", "pass", "", "");
    Article article = new Article("title", "desc", "body", Arrays.asList(), author.getId());
    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(
        AuthorizationService.canWriteComment(articleAuthor, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(
        AuthorizationService.canWriteComment(commentAuthor, article, comment), is(true));
  }

  @Test
  public void should_deny_unrelated_user_to_write_comment() {
    User articleAuthor = new User("a@b.com", "author", "pass", "", "");
    User commentAuthor = new User("c@d.com", "commenter", "pass", "", "");
    User unrelated = new User("e@f.com", "unrelated", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), articleAuthor.getId());
    Comment comment = new Comment("body", commentAuthor.getId(), article.getId());
    assertThat(AuthorizationService.canWriteComment(unrelated, article, comment), is(false));
  }
}
