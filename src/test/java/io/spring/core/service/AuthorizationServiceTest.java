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

  private User articleOwner;
  private User commentOwner;
  private User otherUser;
  private Article article;
  private Comment commentByArticleOwner;
  private Comment commentByCommentOwner;

  @BeforeEach
  public void setUp() {
    articleOwner = new User("owner@example.com", "articleowner", "pass", "bio", "img.jpg");
    commentOwner = new User("commenter@example.com", "commentowner", "pass", "bio", "img.jpg");
    otherUser = new User("other@example.com", "otheruser", "pass", "bio", "img.jpg");

    article =
        new Article("Test Title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    commentByArticleOwner =
        new Comment("Comment by article owner", articleOwner.getId(), article.getId());
    commentByCommentOwner =
        new Comment("Comment by comment owner", commentOwner.getId(), article.getId());
  }

  @Test
  public void should_allow_article_owner_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(articleOwner, article);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(otherUser, article);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_article_owner_to_write_any_comment_on_their_article() {
    boolean canWrite =
        AuthorizationService.canWriteComment(articleOwner, article, commentByCommentOwner);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_allow_comment_owner_to_write_their_comment() {
    boolean canWrite =
        AuthorizationService.canWriteComment(commentOwner, article, commentByCommentOwner);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_comment() {
    boolean canWrite =
        AuthorizationService.canWriteComment(otherUser, article, commentByCommentOwner);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_article_owner_to_write_their_own_comment() {
    boolean canWrite =
        AuthorizationService.canWriteComment(articleOwner, article, commentByArticleOwner);

    assertThat(canWrite, is(true));
  }
}
