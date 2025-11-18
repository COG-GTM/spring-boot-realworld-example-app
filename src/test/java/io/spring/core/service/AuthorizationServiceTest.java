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
  public void should_allow_user_to_write_own_article() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    Article article =
        new Article("title", "description", "body", Arrays.asList("tag1"), user.getId());

    assertThat(AuthorizationService.canWriteArticle(user, article), is(true));
  }

  @Test
  public void should_not_allow_user_to_write_other_users_article() {
    User user1 = new User("test1@example.com", "user1", "password", "bio", "image");
    User user2 = new User("test2@example.com", "user2", "password", "bio", "image");
    Article article =
        new Article("title", "description", "body", Arrays.asList("tag1"), user2.getId());

    assertThat(AuthorizationService.canWriteArticle(user1, article), is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    User articleAuthor =
        new User("author@example.com", "author", "password", "bio", "image");
    User commenter =
        new User("commenter@example.com", "commenter", "password", "bio", "image");
    Article article =
        new Article("title", "description", "body", Arrays.asList("tag1"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(articleAuthor, article, comment), is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    User articleAuthor =
        new User("author@example.com", "author", "password", "bio", "image");
    User commenter =
        new User("commenter@example.com", "commenter", "password", "bio", "image");
    Article article =
        new Article("title", "description", "body", Arrays.asList("tag1"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(commenter, article, comment), is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_comment() {
    User articleAuthor =
        new User("author@example.com", "author", "password", "bio", "image");
    User commenter =
        new User("commenter@example.com", "commenter", "password", "bio", "image");
    User otherUser = new User("other@example.com", "other", "password", "bio", "image");
    Article article =
        new Article("title", "description", "body", Arrays.asList("tag1"), articleAuthor.getId());
    Comment comment = new Comment("comment body", commenter.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(otherUser, article, comment), is(false));
  }
}
