package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.TestHelper;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest extends GraphQLTestBase {

  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentDatafetcher commentDatafetcher;

  private CommentData commentData(String articleId) {
    return new CommentData(
        "comment-id",
        "comment body",
        articleId,
        new DateTime(),
        new DateTime(),
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  @Test
  void should_get_comment_from_local_context() {
    CommentData commentData = commentData("article-id");

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgsDfe(null, commentData));

    assertThat(result.getData().getId()).isEqualTo(commentData.getId());
    assertThat(result.getData().getBody()).isEqualTo(commentData.getBody());
    assertThat(result.getData().getCreatedAt()).isEqualTo(result.getData().getUpdatedAt());
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsKey(commentData.getId());
  }

  @Test
  void should_get_article_comments_forward() {
    ArticleData articleData = TestHelper.articleDataFixture("test", user);
    CommentData commentData = commentData(articleData.getId());
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10,
            null,
            null,
            null,
            dgsDfe(
                io.spring.graphql.types.Article.newBuilder().slug(articleData.getSlug()).build(),
                Collections.singletonMap(articleData.getSlug(), articleData)));

    CommentsConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(1);
    assertThat(connection.getEdges().get(0).getNode().getId()).isEqualTo(commentData.getId());
    assertThat(connection.getEdges().get(0).getCursor())
        .isEqualTo(commentData.getCursor().toString());
    assertThat(connection.getPageInfo().isHasNextPage()).isFalse();
    assertThat(connection.getPageInfo().getStartCursor().getValue())
        .isEqualTo(commentData.getCursor().toString());
    @SuppressWarnings("unchecked")
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    assertThat(localContext).containsKey(commentData.getId());
  }

  @Test
  void should_get_article_comments_backward() {
    ArticleData articleData = TestHelper.articleDataFixture("test", user);
    CommentData commentData = commentData(articleData.getId());
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            null,
            null,
            10,
            String.valueOf(new DateTime().getMillis()),
            dgsDfe(
                io.spring.graphql.types.Article.newBuilder().slug(articleData.getSlug()).build(),
                Collections.singletonMap(articleData.getSlug(), articleData)));

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_return_empty_page_info_when_no_comments() {
    ArticleData articleData = TestHelper.articleDataFixture("test", user);
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10,
            null,
            null,
            null,
            dgsDfe(
                io.spring.graphql.types.Article.newBuilder().slug(articleData.getSlug()).build(),
                Collections.singletonMap(articleData.getSlug(), articleData)));

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
  }

  @Test
  void should_reject_article_comments_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> commentDatafetcher.articleComments(null, null, null, null, dgsDfe(null, null)));
  }
}
