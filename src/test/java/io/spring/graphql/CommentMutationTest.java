package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      ArticleDatafetcher.class
    })
public class CommentMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  private User author;
  private User commenter;
  private Article article;
  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    author = new User("author@test.com", "author", "123", "bio", "image");
    commenter = new User("commenter@test.com", "commenter", "123", "", "");
    article = new Article("Test Article", "desc", "body", Arrays.asList("java"), author.getId());
    articleData = TestHelper.getArticleDataFromArticleAndUser(article, author);
    DateTime now = new DateTime();
    commentData =
        new CommentData(
            "comment-1",
            "nice article",
            article.getId(),
            now,
            now,
            new ProfileData(commenter.getId(), commenter.getUsername(), "", "", false));
    authenticate(commenter);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_add_a_comment_to_an_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(commenter))).thenReturn(Optional.of(commentData));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"nice article\") { comment { id body"
                    + " createdAt } } }",
                article.getSlug()));

    assertThat(result.<String>read("data.addComment.comment.id")).isEqualTo(commentData.getId());
    assertThat(result.<String>read("data.addComment.comment.body"))
        .isEqualTo(commentData.getBody());
    assertThat(result.<String>read("data.addComment.comment.createdAt"))
        .isEqualTo(ISODateTimeFormat.dateTime().withZoneUTC().print(commentData.getCreatedAt()));

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("nice article");
    assertThat(captor.getValue().getUserId()).isEqualTo(commenter.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  public void should_reject_adding_a_comment_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"whatever\", body: \"hi\") { comment { id } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_report_error_when_commenting_on_an_unknown_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"missing\", body: \"hi\") { comment { id } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_report_error_when_the_saved_comment_cannot_be_read_back() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(commenter))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"hi\") { comment { id } } }",
                article.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
    verify(commentRepository).save(any());
  }

  @Test
  public void should_delete_a_comment_written_by_the_current_user() {
    Comment comment = new Comment("nice article", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()));

    assertThat(result.<Boolean>read("data.deleteComment.success")).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_let_the_article_author_delete_any_comment() {
    Comment comment = new Comment("nice article", commenter.getId(), article.getId());
    authenticate(author);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DocumentContext result =
        dgsQueryExecutor.executeAndGetDocumentContext(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()));

    assertThat(result.<Boolean>read("data.deleteComment.success")).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_reject_deleting_a_comment_of_somebody_else() {
    Comment comment = new Comment("nice article", commenter.getId(), article.getId());
    authenticate(new User("other@test.com", "other", "123", "", ""));
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_report_error_when_deleting_an_unknown_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("missing")))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"missing\") { success } }",
                article.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_report_error_when_deleting_a_comment_of_an_unknown_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \"missing\", id: \"comment-1\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  public void should_reject_deleting_a_comment_without_a_current_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \"whatever\", id: \"comment-1\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_fail_to_resolve_the_article_of_a_comment() {
    // known bug: CommentDatafetcher publishes a Map<String, CommentData> as local context while
    // ArticleDatafetcher#getCommentArticle expects a bare CommentData, so Comment.article always
    // blows up with a ClassCastException
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(commenter))).thenReturn(Optional.of(commentData));
    when(articleQueryService.findById(eq(article.getId()), eq(commenter)))
        .thenReturn(Optional.of(articleData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"nice article\") { comment { id"
                    + " article { slug } } } }",
                article.getSlug()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ClassCastException");
    assertThat(result.getErrors().get(0).getPath())
        .containsExactly("addComment", "comment", "article");
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
