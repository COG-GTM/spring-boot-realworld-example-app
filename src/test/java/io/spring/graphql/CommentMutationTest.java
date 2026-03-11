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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentMutation.class, CommentDatafetcher.class})
public class CommentMutationTest {

  @Autowired
  private DgsQueryExecutor dgsQueryExecutor;

  @MockBean
  private ArticleRepository articleRepository;

  @MockBean
  private CommentRepository commentRepository;

  @MockBean
  private CommentQueryService commentQueryService;

  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    user = new User("user@example.com", "testuser", "password", "bio", "image");
    DateTime now = new DateTime();
    article = new Article("Test Article", "Test Description", "Test Body", Arrays.asList("java"), user.getId(), now);
    comment = new Comment("Test comment body", user.getId(), article.getId());

    ProfileData profileData = new ProfileData(
        user.getId(),
        user.getUsername(),
        user.getBio(),
        user.getImage(),
        false);
    commentData = new CommentData(
        comment.getId(),
        comment.getBody(),
        article.getId(),
        now,
        now,
        profileData);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, Arrays.asList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAddComment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String mutation = String.format(
        "mutation { addComment(slug: \"%s\", body: \"Test comment body\") { comment { id body } } }",
        article.getSlug());

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment");

    assertThat(result).isNotNull();
    assertThat(result.get("body")).isEqualTo("Test comment body");
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void shouldDeleteComment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation = String.format(
        "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
        article.getSlug(), comment.getId());

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void shouldFailToAddCommentWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = String.format(
        "mutation { addComment(slug: \"%s\", body: \"Test comment\") { comment { id } } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToAddCommentToNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { addComment(slug: \"non-existent\", body: \"Test comment\") { comment { id } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToDeleteNonExistentComment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = String.format(
        "mutation { deleteComment(slug: \"%s\", id: \"non-existent\") { success } }",
        article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void shouldFailToDeleteCommentWithoutAuth() {
    SecurityContextHolder.clearContext();

    String mutation = String.format(
        "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
        article.getSlug(), comment.getId());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
