package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class CommentMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  public void should_add_comment_when_authenticated() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    CommentData commentData = createCommentData(article);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String mutation =
        "mutation { addComment(slug: \"" + article.getSlug() + "\", body: \"This is a comment\") { comment { id body } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment");

    assertThat(result).isNotNull();
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    clearAuthentication();

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { addComment(slug: \"" + article.getSlug() + "\", body: \"This is a comment\") { comment { id body } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_add_comment_to_nonexistent_article() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    String mutation =
        "mutation { addComment(slug: \"nonexistent\", body: \"This is a comment\") { comment { id body } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_delete_comment_when_author() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Test comment", user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \"" + article.getSlug() + "\", id: \"" + comment.getId() + "\") { success } }";

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_fail_delete_comment_when_not_author() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), anotherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Test comment", anotherUser.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \"" + article.getSlug() + "\", id: \"" + comment.getId() + "\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_delete_nonexistent_comment() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(any(), any())).thenReturn(Optional.empty());

    String mutation =
        "mutation { deleteComment(slug: \"" + article.getSlug() + "\", id: \"nonexistent\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_delete_comment_without_authentication() {
    clearAuthentication();

    String mutation = "mutation { deleteComment(slug: \"test-slug\", id: \"comment-id\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  private CommentData createCommentData(Article article) {
    DateTime now = new DateTime();
    return new CommentData(
        "comment-id",
        "This is a comment",
        article.getId(),
        now,
        now,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }
}
