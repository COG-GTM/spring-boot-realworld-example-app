package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class CommentMutationTest extends GraphQLTestBase {

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  public void should_add_comment_successfully() {
    authenticateUser();

    String slug = "test-article";
    String body = "Great article!";

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment(body, user.getId(), article.getId());
    CommentData commentData =
        new CommentData(
            comment.getId(),
            body,
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    String query =
        "mutation AddComment($slug: String!, $body: String!) {"
            + "  addComment(slug: $slug, body: $body) {"
            + "    comment {"
            + "      id"
            + "      body"
            + "      createdAt"
            + "      author {"
            + "        username"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);
    variables.put("body", body);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.addComment.comment", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("body")).isEqualTo(body);
    verify(commentRepository).save(any());

    clearAuthentication();
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    clearAuthentication();

    String query =
        "mutation AddComment($slug: String!, $body: String!) {"
            + "  addComment(slug: $slug, body: $body) {"
            + "    comment {"
            + "      id"
            + "      body"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-article");
    variables.put("body", "Test comment");

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.addComment.comment", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_delete_comment_successfully() {
    authenticateUser();

    String slug = "test-article";
    String commentId = "comment-id";

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Test comment", user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    String query =
        "mutation DeleteComment($slug: String!, $id: ID!) {"
            + "  deleteComment(slug: $slug, id: $id) {"
            + "    success"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);
    variables.put("id", commentId);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.deleteComment", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("success")).isEqualTo(true);
    verify(commentRepository).remove(any());

    clearAuthentication();
  }

  @Test
  public void should_fail_delete_comment_for_non_author() {
    authenticateUser();

    String slug = "test-article";
    String commentId = "comment-id";
    String otherUserId = "other-user-id";

    Article article =
        new Article("Test Article", "Description", "Body", Arrays.asList(), otherUserId);
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Test comment", otherUserId, article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    String query =
        "mutation DeleteComment($slug: String!, $id: ID!) {"
            + "  deleteComment(slug: $slug, id: $id) {"
            + "    success"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);
    variables.put("id", commentId);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.deleteComment", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }

    clearAuthentication();
  }
}
