package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.TestHelper;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest extends GraphqlTestBase {

  @Mock private CommentQueryService commentQueryService;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor;

  @InjectMocks private CommentDatafetcher commentDatafetcher;

  private User user;
  private ArticleData articleData;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    articleData = TestHelper.articleDataFixture("1", user);
    commentData =
        new CommentData(
            "comment-id",
            "comment body",
            articleData.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), user.getUsername(), "", "", false));
  }

  private void sourceArticle() {
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put(articleData.getSlug(), articleData);
    when(environment.getSource())
        .thenReturn(Article.newBuilder().slug(articleData.getSlug()).build());
    when(environment.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
  }

  @Test
  void should_build_comment_of_a_payload() {
    when(environment.<CommentData>getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgsEnvironment);

    assertThat(result.getData().getId()).isEqualTo(commentData.getId());
    assertThat(result.getData().getBody()).isEqualTo(commentData.getBody());
    Map<String, CommentData> localContext = asMap(result.getLocalContext());
    assertThat(localContext).containsEntry(commentData.getId(), commentData);
  }

  @Test
  void should_get_comments_of_an_article_forward() {
    login(user);
    sourceArticle();
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, "1000", null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getBody())
        .isEqualTo(commentData.getBody());
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    Map<String, CommentData> localContext = asMap(result.getLocalContext());
    assertThat(localContext).containsEntry(commentData.getId(), commentData);

    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), eq(user), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  void should_get_comments_of_an_article_backward_as_anonymous_user() {
    logout();
    sourceArticle();
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.PREV, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, "2000", dgsEnvironment);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();

    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), isNull(), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  void should_reject_comments_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> commentDatafetcher.articleComments(null, null, null, null, dgsEnvironment));
  }
}
