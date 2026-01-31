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

  private User articleAuthor;
  private User commentAuthor;
  private User unrelatedUser;
  private Article article;
  private Comment comment;

  @BeforeEach
  public void setUp() {
    articleAuthor = new User("author@example.com", "articleauthor", "password", "bio", "image");
    commentAuthor = new User("commenter@example.com", "commentauthor", "password", "bio", "image");
    unrelatedUser = new User("unrelated@example.com", "unrelateduser", "password", "bio", "image");

    article =
        new Article(
            "Test Article", "Description", "Body", Arrays.asList("java"), articleAuthor.getId());
    comment = new Comment("Test comment", commentAuthor.getId(), article.getId());
  }

  @Test
  public void should_allow_article_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(articleAuthor, article);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_non_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(unrelatedUser, article);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_not_allow_comment_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(commentAuthor, article);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_article_author_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(articleAuthor, article, comment);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(commentAuthor, article, comment);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_unrelated_user_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(unrelatedUser, article, comment);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_user_who_is_both_article_and_comment_author() {
    Comment authorComment = new Comment("Author's comment", articleAuthor.getId(), article.getId());

    boolean canWrite = AuthorizationService.canWriteComment(articleAuthor, article, authorComment);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_handle_different_articles_for_same_author() {
    Article anotherArticle =
        new Article(
            "Another Article", "Desc", "Body", Arrays.asList("spring"), articleAuthor.getId());

    boolean canWriteFirst = AuthorizationService.canWriteArticle(articleAuthor, article);
    boolean canWriteSecond = AuthorizationService.canWriteArticle(articleAuthor, anotherArticle);

    assertThat(canWriteFirst, is(true));
    assertThat(canWriteSecond, is(true));
  }

  @Test
  public void should_handle_different_comments_on_same_article() {
    Comment anotherComment = new Comment("Another comment", unrelatedUser.getId(), article.getId());

    boolean canWriteFirst = AuthorizationService.canWriteComment(commentAuthor, article, comment);
    boolean canWriteSecond =
        AuthorizationService.canWriteComment(unrelatedUser, article, anotherComment);

    assertThat(canWriteFirst, is(true));
    assertThat(canWriteSecond, is(true));
  }

  @Test
  public void should_not_allow_cross_user_comment_deletion() {
    Comment anotherComment = new Comment("Another comment", unrelatedUser.getId(), article.getId());

    boolean canWrite = AuthorizationService.canWriteComment(commentAuthor, article, anotherComment);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_article_author_to_delete_any_comment_on_their_article() {
    Comment commentByUnrelated =
        new Comment("Comment by unrelated", unrelatedUser.getId(), article.getId());

    boolean canWrite =
        AuthorizationService.canWriteComment(articleAuthor, article, commentByUnrelated);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_handle_multiple_users_commenting_on_same_article() {
    Comment comment1 = new Comment("Comment 1", commentAuthor.getId(), article.getId());
    Comment comment2 = new Comment("Comment 2", unrelatedUser.getId(), article.getId());

    boolean author1CanWriteComment1 =
        AuthorizationService.canWriteComment(commentAuthor, article, comment1);
    boolean author1CanWriteComment2 =
        AuthorizationService.canWriteComment(commentAuthor, article, comment2);
    boolean author2CanWriteComment1 =
        AuthorizationService.canWriteComment(unrelatedUser, article, comment1);
    boolean author2CanWriteComment2 =
        AuthorizationService.canWriteComment(unrelatedUser, article, comment2);

    assertThat(author1CanWriteComment1, is(true));
    assertThat(author1CanWriteComment2, is(false));
    assertThat(author2CanWriteComment1, is(false));
    assertThat(author2CanWriteComment2, is(true));
  }

  @Test
  public void should_verify_article_ownership_by_user_id() {
    User userWithSameCredentials =
        new User("author@example.com", "articleauthor", "password", "bio", "image");

    boolean canWrite = AuthorizationService.canWriteArticle(userWithSameCredentials, article);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_verify_comment_ownership_by_user_id() {
    User userWithSameCredentials =
        new User("commenter@example.com", "commentauthor", "password", "bio", "image");

    boolean canWrite =
        AuthorizationService.canWriteComment(userWithSameCredentials, article, comment);

    assertThat(canWrite, is(false));
  }
}
