package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, CommentDatafetcher.class, ArticleDatafetcher.class})
public class CommentDatafetcherTest extends GraphQLTestBase {

  private static final DateTime TIME = new DateTime(2022, 2, 2, 10, 0, DateTimeZone.UTC);
  private static final String TIME_ISO = "2022-02-02T10:00:00.000Z";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private io.spring.core.user.UserRepository userRepository;

  @Test
  void should_return_article_comments_paginated_forward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData("1")), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments(first: 5, after: \"1000\") { edges { cursor node { id body"
                + " createdAt updatedAt } } pageInfo { hasNextPage hasPreviousPage startCursor"
                + " endCursor } } } }");

    assertThat(context.read("$.data.article.comments.edges[0].node.id", String.class))
        .isEqualTo("comment-1");
    assertThat(context.read("$.data.article.comments.edges[0].node.body", String.class))
        .isEqualTo("body 1");
    assertThat(context.read("$.data.article.comments.edges[0].node.createdAt", String.class))
        .isEqualTo(TIME_ISO);
    assertThat(context.read("$.data.article.comments.edges[0].cursor", String.class))
        .isEqualTo(String.valueOf(TIME.getMillis()));
    assertThat(context.read("$.data.article.comments.pageInfo.hasNextPage", Boolean.class))
        .isTrue();
    assertThat(context.read("$.data.article.comments.pageInfo.endCursor", String.class))
        .isEqualTo(String.valueOf(TIME.getMillis()));

    CursorPageParameter<DateTime> pageParameter = capturePageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(5);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  void should_return_article_comments_paginated_backward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments(last: 2, before: \"2000\") { edges { cursor } pageInfo {"
                + " hasPreviousPage startCursor } } } }");

    assertThat(context.read("$.data.article.comments.edges", List.class)).isEmpty();
    assertThat(context.read("$.data.article.comments.pageInfo.hasPreviousPage", Boolean.class))
        .isFalse();
    assertThat(context.read("$.data.article.comments.pageInfo.startCursor", String.class)).isNull();

    CursorPageParameter<DateTime> pageParameter = capturePageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(2);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  void should_query_comments_for_anonymous_user() {
    logout();
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), isNull()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments(first: 1) { edges { cursor } } } }");

    assertThat(result.getErrors()).isEmpty();
    verify(commentQueryService).findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any());
  }

  @Test
  void should_fail_comments_without_first_or_last() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ article(slug: \""
                + articleData.getSlug()
                + "\") { comments { edges { cursor } } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  private CursorPageParameter<DateTime> capturePageParameter() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService).findByArticleIdWithCursor(any(), any(), captor.capture());
    return captor.getValue();
  }

  private ArticleData articleData() {
    return new ArticleData(
        "article-id",
        "a-title",
        "a title",
        "a description",
        "a body",
        false,
        0,
        TIME,
        TIME,
        Arrays.asList("joda"),
        profileData);
  }

  private CommentData commentData(String seed) {
    return new CommentData(
        "comment-" + seed, "body " + seed, "article-id", TIME, TIME, profileData);
  }
}
