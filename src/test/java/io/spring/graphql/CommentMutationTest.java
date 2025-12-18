package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
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
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    DateTime now = new DateTime();
    article =
        new Article(
            "Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId(), now);

    comment = new Comment("Test comment body", user.getId(), article.getId());

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData =
        new CommentData(
            comment.getId(),
            comment.getBody(),
            article.getId(),
            now,
            now,
            profileData);
  }

  @Test
  public void should_add_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String mutation =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"Test comment body\") { comment { id body } } }",
            article.getSlug());

    String body =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.body");

    assert body.equals("Test comment body");
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_delete_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            article.getSlug(), comment.getId());

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assert success;
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_fail_add_comment_without_auth() {
    SecurityContextHolder.clearContext();

    String mutation =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"Test comment\") { comment { id } } }",
            article.getSlug());

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.id");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_fail_add_comment_to_nonexistent_article() {
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { addComment(slug: \"nonexistent\", body: \"Test comment\") { comment { id } } }";

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.id");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }

  @Test
  public void should_fail_delete_nonexistent_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("nonexistent")))
        .thenReturn(Optional.empty());

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"nonexistent\") { success } }",
            article.getSlug());

    try {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");
      assert false : "Should have thrown exception";
    } catch (Exception e) {
      assert true;
    }
  }
}
