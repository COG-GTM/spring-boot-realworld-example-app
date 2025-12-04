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
  private User otherUser;
  private Article article;
  private Comment comment;

  @BeforeEach
  public void setUp() {
    articleAuthor = new User("author@email.com", "author", "password", "", "");
    commentAuthor = new User("commenter@email.com", "commenter", "password", "", "");
    otherUser = new User("other@email.com", "other", "password", "", "");

    article = new Article("title", "desc", "body", Arrays.asList("java"), articleAuthor.getId());
    comment = new Comment("comment body", commentAuthor.getId(), article.getId());
  }

  @Test
  public void should_allow_article_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(articleAuthor, article);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_other_user_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(otherUser, article);

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
  public void should_not_allow_other_user_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(otherUser, article, comment);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_user_who_is_both_article_and_comment_author() {
    Comment authorComment = new Comment("body", articleAuthor.getId(), article.getId());
    boolean canWrite = AuthorizationService.canWriteComment(articleAuthor, article, authorComment);

    assertThat(canWrite, is(true));
  }
}
