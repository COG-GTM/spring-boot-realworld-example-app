package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class
    })
public class CommentMutationTest extends DgsTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ProfileQueryService profileQueryService;
  @MockBean private UserRepository userRepository;

  private User user;
  private Article article;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    user = TestHelper.userFixture("commenter");
    article = TestHelper.articleFixture("commented", user);
    commentData = TestHelper.commentDataFixture("new", user);
    authenticate(user);
  }

  @Test
  public void should_add_comment_to_an_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));
    when(profileQueryService.findByUsername(eq(user.getUsername()), eq(user)))
        .thenReturn(Optional.of(TestHelper.profileDataFixture(user)));

    Map<String, Object> comment =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"nice one\") { comment { id body createdAt author { username } } }"
                + " }",
            "data.addComment.comment");

    assertThat(comment.get("id")).isEqualTo(commentData.getId());
    assertThat(comment.get("body")).isEqualTo(commentData.getBody());
    assertThat(((Map<String, Object>) comment.get("author")).get("username"))
        .isEqualTo(user.getUsername());

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("nice one");
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  public void should_not_add_comment_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"any\", body: \"b\") { comment { id } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_not_add_comment_to_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"unknown\", body: \"b\") { comment { id } } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_fail_when_saved_comment_cannot_be_read_back() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"b\") { comment {"
                + " id } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_delete_own_comment() {
    Comment comment = TestHelper.commentFixture("mine", article, user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \""
                + comment.getId()
                + "\") { success } }",
            "data.deleteComment.success");

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_not_delete_comment_of_another_user_on_another_users_article() {
    User other = TestHelper.userFixture("other");
    Article othersArticle = TestHelper.articleFixture("others", other);
    Comment othersComment = TestHelper.commentFixture("others", othersArticle, other);
    when(articleRepository.findBySlug(eq(othersArticle.getSlug())))
        .thenReturn(Optional.of(othersArticle));
    when(commentRepository.findById(eq(othersArticle.getId()), eq(othersComment.getId())))
        .thenReturn(Optional.of(othersComment));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + othersArticle.getSlug()
                + "\", id: \""
                + othersComment.getId()
                + "\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_not_delete_unknown_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("unknown")))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \"unknown\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_not_delete_comment_of_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \"unknown\", id: \"1\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_not_delete_comment_for_anonymous_user() {
    authenticateAnonymously();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \"any\", id: \"1\") { success } }");

    assertThat(result.getErrors()).isNotEmpty();
    verify(commentRepository, never()).remove(any());
  }

  /**
   * The {@code Comment.article} datafetcher expects the local context to be a {@link CommentData},
   * but every parent datafetcher publishes a map keyed by comment id, so the field always fails.
   */
  @Test
  public void should_fail_resolving_the_article_of_a_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"b\") { comment { id article { slug } } } }");

    assertThat(result.getErrors()).isNotEmpty();
  }
}
