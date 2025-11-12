package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class
    })
public class CommentMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article = new Article("Test Title", "Test Description", "Test Body", null, user.getId(), new DateTime());
    comment = new Comment("Test comment body", user.getId(), article.getId());
    
    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData = new CommentData(comment.getId(), comment.getBody(), article.getId(), comment.getCreatedAt(), comment.getCreatedAt(), profileData);
    
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
  }

  @Test
  public void should_add_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    String mutation =
        "mutation AddComment($slug: String!, $body: String!) { "
            + "  addComment(slug: $slug, body: $body) { "
            + "    comment { "
            + "      id "
            + "      body "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");
    variables.put("body", "Test comment body");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> addComment = (Map<String, Object>) data.get("addComment");
    assertNotNull(addComment);

    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_fail_add_comment_when_article_not_found() {
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation AddComment($slug: String!, $body: String!) { "
            + "  addComment(slug: $slug, body: $body) { "
            + "    comment { "
            + "      id "
            + "      body "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "nonexistent");
    variables.put("body", "Test comment body");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_delete_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("comment-id")))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation DeleteComment($slug: String!, $id: ID!) { "
            + "  deleteComment(slug: $slug, id: $id) { "
            + "    success "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");
    variables.put("id", "comment-id");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> deleteComment = (Map<String, Object>) data.get("deleteComment");
    assertNotNull(deleteComment);
    assertEquals(true, deleteComment.get("success"));

    verify(commentRepository).remove(any(Comment.class));
  }

  @Test
  public void should_fail_delete_comment_when_article_not_found() {
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation DeleteComment($slug: String!, $id: ID!) { "
            + "  deleteComment(slug: $slug, id: $id) { "
            + "    success "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "nonexistent");
    variables.put("id", "comment-id");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_fail_delete_comment_when_comment_not_found() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("nonexistent")))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation DeleteComment($slug: String!, $id: ID!) { "
            + "  deleteComment(slug: $slug, id: $id) { "
            + "    success "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");
    variables.put("id", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }
}
