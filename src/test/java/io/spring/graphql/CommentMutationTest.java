package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentMutation.class, CommentDatafetcher.class})
public class CommentMutationTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  public void should_add_comment_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    String commentBody = "This is a test comment";

    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData(user.getId(), username, "", defaultAvatar, false);
    CommentData commentData = new CommentData("comment-id", commentBody, article.getId(), now, now, profileData);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String mutation =
        "mutation { addComment(slug: \""
            + article.getSlug()
            + "\", body: \""
            + commentBody
            + "\") { comment { id body } } }";

    String resultId =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.id");
    String resultBody =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.body");

    assertThat(resultId).isEqualTo("comment-id");
    assertThat(resultBody).isEqualTo(commentBody);
  }

  @Test
  public void should_fail_add_comment_when_not_authenticated() {
    clearAuthentication();

    String mutation =
        "mutation { addComment(slug: \"test-slug\", body: \"comment\") { comment { id } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_delete_comment_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    Comment comment = new Comment("Comment body", user.getId(), article.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \""
            + article.getSlug()
            + "\", id: \""
            + comment.getId()
            + "\") { success } }";

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_fail_delete_comment_when_not_author() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    setAuthenticatedUser(anotherUser);

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    Comment comment = new Comment("Comment body", user.getId(), article.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \""
            + article.getSlug()
            + "\", id: \""
            + comment.getId()
            + "\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_delete_comment_when_article_not_found() {
    setAuthenticatedUser(user);

    when(articleRepository.findBySlug(eq("nonexistent-slug"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { deleteComment(slug: \"nonexistent-slug\", id: \"comment-id\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
