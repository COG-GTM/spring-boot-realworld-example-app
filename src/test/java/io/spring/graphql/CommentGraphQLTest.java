package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentMutation.class, CommentDatafetcher.class, ProfileDatafetcher.class})
@ActiveProfiles("test")
public class CommentGraphQLTest extends GraphQLTestBase {

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  void should_add_comment_when_authenticated() {
    setAuthenticatedUser(user);

    String slug = "test-article";
    String commentBody = "This is a test comment";

    Article article = new Article("Test Article", "Description", "Body", Collections.emptyList(), user.getId());
    Comment comment = new Comment(commentBody, user.getId(), article.getId());

    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    CommentData commentData = new CommentData(comment.getId(), commentBody, article.getId(), new DateTime(), new DateTime(), profileData);

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any())).thenReturn(Optional.of(profileData));

    String mutation =
        "mutation { addComment(slug: \"" + slug + "\", body: \"" + commentBody + "\") { comment { id body author { username } } } }";

    String resultId = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.id");

    assertThat(resultId).isNotNull();
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_delete_comment_when_author() {
    setAuthenticatedUser(user);

    String slug = "test-article";
    String commentId = "comment-123";

    Article article = new Article("Test Article", "Description", "Body", Collections.emptyList(), user.getId());
    Comment comment = new Comment("Comment body", user.getId(), article.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(commentId))).thenReturn(Optional.of(comment));

    String mutation =
        "mutation { deleteComment(slug: \"" + slug + "\", id: \"" + commentId + "\") { success } }";

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }
}
