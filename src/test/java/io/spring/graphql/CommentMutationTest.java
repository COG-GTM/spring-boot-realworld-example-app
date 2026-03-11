package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class CommentMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @MockBean private UserRepository userRepository;

  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    user = new User(
        "test@example.com",
        "testuser",
        "password",
        "bio",
        "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null));

    article =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1"),
            user.getId());

    comment = new Comment("Test comment body", user.getId(), article.getId());

    DateTime now = new DateTime();
    commentData =
        new CommentData(
            comment.getId(),
            comment.getBody(),
            article.getId(),
            now,
            now,
            new ProfileData(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getImage(),
                false));
  }

  @Test
  public void should_add_comment_success() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user)))
        .thenReturn(Optional.of(commentData));

    String mutation =
        "mutation { addComment(slug: \""
            + article.getSlug()
            + "\", body: \"Test comment body\") "
            + "{ comment { id body } } }";

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.addComment");
    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_delete_comment_success() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentRepository.findById(
            eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \""
            + article.getSlug()
            + "\", id: \""
            + comment.getId()
            + "\") { success } }";

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            mutation, "data.deleteComment.success");
    assertNotNull(success);
    assertTrue(success);
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { addComment(slug: \""
            + article.getSlug()
            + "\", body: \"Test comment\") "
            + "{ comment { id } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.addComment");
        });
  }

  @Test
  public void should_fail_delete_comment_without_authentication() {
    SecurityContextHolder.clearContext();

    String mutation =
        "mutation { deleteComment(slug: \""
            + article.getSlug()
            + "\", id: \""
            + comment.getId()
            + "\") { success } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.deleteComment");
        });
  }

  @Test
  public void should_fail_add_comment_to_nonexistent_article() {
    when(articleRepository.findBySlug(eq("nonexistent-slug")))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { addComment(slug: \"nonexistent-slug\", "
            + "body: \"Test comment\") { comment { id } } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.addComment");
        });
  }

  @Test
  public void should_fail_delete_nonexistent_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentRepository.findById(
            eq(article.getId()), eq("nonexistent-id")))
        .thenReturn(Optional.empty());

    String mutation =
        "mutation { deleteComment(slug: \""
            + article.getSlug()
            + "\", id: \"nonexistent-id\") { success } }";

    assertThrows(
        Exception.class,
        () -> {
          dgsQueryExecutor.executeAndExtractJsonPath(
              mutation, "data.deleteComment");
        });
  }

}
