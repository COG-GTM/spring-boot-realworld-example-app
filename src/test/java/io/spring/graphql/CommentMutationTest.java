package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
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
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      ProfileDatafetcher.class,
      WebSecurityConfig.class,
      BCryptPasswordEncoder.class,
      JacksonCustomizations.class
    })
public class CommentMutationTest {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  private String defaultAvatar;
  private User user;
  private ProfileData profileData;
  private Article article;

  @BeforeEach
  public void setUp() {
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";
    user = new User("john@jacob.com", "johnjacob", "password", "bio", defaultAvatar);
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    article = new Article("How to train your dragon", "Ever wonder how?", "You have to believe", Arrays.asList("reactjs"), user.getId());
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_add_comment_successfully() {
    String slug = "how-to-train-your-dragon";
    String commentBody = "Great article!";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment(commentBody, user.getId(), article.getId());
    CommentData commentData = new CommentData(
        comment.getId(),
        commentBody,
        article.getId(),
        new DateTime(),
        new DateTime(),
        profileData);

    when(commentQueryService.findById(eq(comment.getId()), any())).thenReturn(Optional.of(commentData));

    String query =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"%s\") { comment { id body createdAt updatedAt author { username bio image following } } } }",
            slug, commentBody);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.addComment.comment", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("body")).isEqualTo(commentBody);
    assertThat(result.get("id")).isNotNull();

    Map<String, Object> author = (Map<String, Object>) result.get("author");
    assertThat(author.get("username")).isEqualTo(user.getUsername());

    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_delete_comment_successfully() {
    String slug = "how-to-train-your-dragon";
    String commentId = "comment-123";
    String commentBody = "Great article!";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    Comment comment = new Comment(commentBody, user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(commentId))).thenReturn(Optional.of(comment));

    String query =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            slug, commentId);

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.deleteComment", Map.class);

    assertThat(result).isNotNull();
    assertThat(result.get("success")).isEqualTo(true);

    verify(commentRepository).remove(any(Comment.class));
  }

  @Test
  public void should_fail_to_add_comment_without_authentication() {
    String slug = "how-to-train-your-dragon";
    String commentBody = "Great article!";

    String query =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"%s\") { comment { id body } } }",
            slug, commentBody);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.addComment.comment", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("AuthenticationException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_fail_to_add_comment_to_nonexistent_article() {
    String slug = "nonexistent-article";
    String commentBody = "Great article!";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"%s\") { comment { id body } } }",
            slug, commentBody);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.addComment.comment", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }

  @Test
  @WithMockUser(username = "johnjacob")
  public void should_fail_to_delete_nonexistent_comment() {
    String slug = "how-to-train-your-dragon";
    String commentId = "nonexistent-comment";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(commentId))).thenReturn(Optional.empty());

    String query =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            slug, commentId);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(query, "data.deleteComment", Map.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("ResourceNotFoundException");
    }
  }
}
