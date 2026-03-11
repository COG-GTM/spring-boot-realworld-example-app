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
  private Comment commentByArticleAuthor;
  private Comment commentByOtherUser;

  @BeforeEach
  public void setUp() {
    articleAuthor = new User("author@email.com", "articleauthor", "password", "bio", "image");
    commentAuthor = new User("commenter@email.com", "commenter", "password", "bio", "image");
    otherUser = new User("other@email.com", "otheruser", "password", "bio", "image");

    article =
        new Article("Test Article", "Description", "Body", Arrays.asList("java"), articleAuthor.getId());

    commentByArticleAuthor =
        new Comment("Comment by article author", articleAuthor.getId(), article.getId());
    commentByOtherUser = new Comment("Comment by other user", commentAuthor.getId(), article.getId());
  }

  @Test
  public void should_allow_article_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(articleAuthor, article);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_non_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(otherUser, article);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_article_author_to_write_any_comment_on_their_article() {
    boolean canWriteOwnComment =
        AuthorizationService.canWriteComment(articleAuthor, article, commentByArticleAuthor);
    boolean canWriteOthersComment =
        AuthorizationService.canWriteComment(articleAuthor, article, commentByOtherUser);

    assertThat(canWriteOwnComment, is(true));
    assertThat(canWriteOthersComment, is(true));
  }

  @Test
  public void should_allow_comment_author_to_write_their_own_comment() {
    boolean canWrite =
        AuthorizationService.canWriteComment(commentAuthor, article, commentByOtherUser);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_not_allow_user_to_write_comment_they_did_not_create_on_others_article() {
    boolean canWrite =
        AuthorizationService.canWriteComment(otherUser, article, commentByOtherUser);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_not_allow_user_to_write_article_authors_comment_on_others_article() {
    boolean canWrite =
        AuthorizationService.canWriteComment(otherUser, article, commentByArticleAuthor);

    assertThat(canWrite, is(false));
  }

  @Test
  public void should_allow_user_who_is_both_article_and_comment_author() {
    Comment ownComment = new Comment("Own comment", articleAuthor.getId(), article.getId());

    boolean canWrite = AuthorizationService.canWriteComment(articleAuthor, article, ownComment);

    assertThat(canWrite, is(true));
  }

  @Test
  public void should_handle_different_articles_by_same_author() {
    Article anotherArticle =
        new Article(
            "Another Article", "Description", "Body", Arrays.asList("java"), articleAuthor.getId());

    boolean canWriteFirstArticle = AuthorizationService.canWriteArticle(articleAuthor, article);
    boolean canWriteSecondArticle =
        AuthorizationService.canWriteArticle(articleAuthor, anotherArticle);

    assertThat(canWriteFirstArticle, is(true));
    assertThat(canWriteSecondArticle, is(true));
  }

  @Test
  public void should_handle_comment_on_different_article() {
    Article anotherArticle =
        new Article("Another Article", "Description", "Body", Arrays.asList("java"), otherUser.getId());
    Comment commentOnAnotherArticle =
        new Comment("Comment", commentAuthor.getId(), anotherArticle.getId());

    boolean canWriteAsArticleAuthor =
        AuthorizationService.canWriteComment(otherUser, anotherArticle, commentOnAnotherArticle);
    boolean canWriteAsCommentAuthor =
        AuthorizationService.canWriteComment(
            commentAuthor, anotherArticle, commentOnAnotherArticle);

    assertThat(canWriteAsArticleAuthor, is(true));
    assertThat(canWriteAsCommentAuthor, is(true));
  }
}
