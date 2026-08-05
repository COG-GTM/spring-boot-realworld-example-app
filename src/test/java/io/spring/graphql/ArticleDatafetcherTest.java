package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class, ProfileDatafetcher.class})
public class ArticleDatafetcherTest extends GraphQLTestBase {

  private static final DateTime TIME = new DateTime(2022, 2, 2, 10, 0, DateTimeZone.UTC);
  private static final String TIME_ISO = "2022-02-02T10:00:00.000Z";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Autowired private ArticleDatafetcher articleDatafetcher;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  @MockBean private ProfileQueryService profileQueryService;

  @Test
  void should_return_user_feed_paginated_forward() {
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData("one")), Direction.NEXT, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(first: 10, after: \"1000\") { edges { cursor node { slug title body description"
                + " favorited favoritesCount tagList createdAt updatedAt } } pageInfo { hasNextPage"
                + " hasPreviousPage startCursor endCursor } } }");

    assertThat(context.read("$.data.feed.edges[0].node.slug", String.class)).isEqualTo("title-one");
    assertThat(context.read("$.data.feed.edges[0].node.title", String.class))
        .isEqualTo("title one");
    assertThat(context.read("$.data.feed.edges[0].node.favorited", Boolean.class)).isTrue();
    assertThat(context.read("$.data.feed.edges[0].node.favoritesCount", Integer.class))
        .isEqualTo(3);
    assertThat(context.read("$.data.feed.edges[0].node.createdAt", String.class))
        .isEqualTo(TIME_ISO);
    assertThat(context.read("$.data.feed.edges[0].node.updatedAt", String.class))
        .isEqualTo(TIME_ISO);
    assertThat(context.read("$.data.feed.edges[0].cursor", String.class))
        .isEqualTo(String.valueOf(TIME.getMillis()));
    assertThat(context.read("$.data.feed.pageInfo.hasNextPage", Boolean.class)).isTrue();
    assertThat(context.read("$.data.feed.pageInfo.hasPreviousPage", Boolean.class)).isFalse();
    assertThat(context.read("$.data.feed.pageInfo.startCursor", String.class))
        .isEqualTo(String.valueOf(TIME.getMillis()));
    assertThat(context.read("$.data.feed.pageInfo.endCursor", String.class))
        .isEqualTo(String.valueOf(TIME.getMillis()));

    CursorPageParameter<DateTime> pageParameter = captureFeedPageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(10);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  void should_return_user_feed_paginated_backward() {
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ feed(last: 3, before: \"2000\") { edges { cursor } pageInfo { hasNextPage"
                + " hasPreviousPage startCursor endCursor } } }");

    assertThat(context.read("$.data.feed.edges", List.class)).isEmpty();
    assertThat(context.read("$.data.feed.pageInfo.hasPreviousPage", Boolean.class)).isTrue();
    assertThat(context.read("$.data.feed.pageInfo.hasNextPage", Boolean.class)).isFalse();
    assertThat(context.read("$.data.feed.pageInfo.startCursor", String.class)).isNull();
    assertThat(context.read("$.data.feed.pageInfo.endCursor", String.class)).isNull();

    CursorPageParameter<DateTime> pageParameter = captureFeedPageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(3);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  void should_query_feed_for_anonymous_user() {
    logout();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    ExecutionResult result = dgsQueryExecutor.execute("{ feed(first: 1) { edges { cursor } } }");

    assertThat(result.getErrors()).isEmpty();
    verify(articleQueryService).findUserFeedWithCursor(isNull(), any());
  }

  @Test
  void should_fail_feed_without_first_or_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ feed { edges { cursor } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  @Test
  void should_return_articles_filtered_by_tag_author_and_favorited_by() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("joda"), eq("johnjacob"), eq("jane"), any(), eq(user)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("two")), Direction.NEXT, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(first: 5, after: \"1000\", authoredBy: \"johnjacob\", favoritedBy:"
                + " \"jane\", withTag: \"joda\") { edges { node { slug title } } pageInfo {"
                + " hasNextPage } } }");

    assertThat(context.read("$.data.articles.edges[0].node.title", String.class))
        .isEqualTo("title two");
    assertThat(context.read("$.data.articles.pageInfo.hasNextPage", Boolean.class)).isFalse();

    CursorPageParameter<DateTime> pageParameter = captureRecentArticlesPageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(5);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  void should_return_articles_paginated_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("three")), Direction.PREV, true));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ articles(last: 2, before: \"3000\") { edges { node { slug } } pageInfo {"
                + " hasPreviousPage } } }");

    assertThat(context.read("$.data.articles.pageInfo.hasPreviousPage", Boolean.class)).isTrue();
    assertThat(context.read("$.data.articles.edges[0].node.slug", String.class))
        .isEqualTo("title-three");

    CursorPageParameter<DateTime> pageParameter = captureRecentArticlesPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getLimit()).isEqualTo(2);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(3000L);
  }

  @Test
  void should_fail_articles_without_first_or_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { cursor } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  @Test
  void should_find_article_by_slug() {
    when(articleQueryService.findBySlug(eq("title-one"), eq(user)))
        .thenReturn(Optional.of(articleData("one")));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ article(slug: \"title-one\") { slug title description body favoritesCount tagList }"
                + " }");

    assertThat(context.read("$.data.article.body", String.class)).isEqualTo("body one");
    assertThat(context.read("$.data.article.description", String.class)).isEqualTo("desc one");
    assertThat(context.read("$.data.article.favoritesCount", Integer.class)).isEqualTo(3);
    assertThat(context.read("$.data.article.tagList", List.class)).containsExactly("joda");
  }

  @Test
  void should_fail_to_find_unknown_article_by_slug() {
    when(articleQueryService.findBySlug(eq("unknown"), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"unknown\") { slug } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_return_profile_feed() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));
    when(userRepository.findByUsername(eq("johnjacob"))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("feed")), Direction.NEXT, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \"johnjacob\") { profile { username feed(first: 2, after:"
                + " \"5000\") { edges { node { slug } } } } } }");

    assertThat(context.read("$.data.profile.profile.feed.edges[0].node.slug", String.class))
        .isEqualTo("title-feed");

    CursorPageParameter<DateTime> pageParameter = captureFeedPageParameter();
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(5000L);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
  }

  @Test
  void should_return_profile_feed_paginated_backward() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));
    when(userRepository.findByUsername(eq("johnjacob"))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"johnjacob\") { profile { feed(last: 4, before: \"6000\") {"
                + " edges { cursor } } } } }");

    assertThat(result.getErrors()).isEmpty();
    CursorPageParameter<DateTime> pageParameter = captureFeedPageParameter();
    assertThat(pageParameter.getLimit()).isEqualTo(4);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(6000L);
  }

  @Test
  void should_fail_profile_feed_for_unknown_user() {
    when(profileQueryService.findByUsername(eq("ghost"), eq(user)))
        .thenReturn(
            Optional.of(new ProfileData("other-id", "ghost", "bio", DEFAULT_AVATAR, false)));
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"ghost\") { profile { feed(first: 2) { edges { cursor } } } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_fail_profile_feed_without_first_or_last() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"johnjacob\") { profile { feed { edges { cursor } } } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  @Test
  void should_return_profile_articles_and_favorites() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), eq(user)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("authored")), Direction.NEXT, false));
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), eq(user)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("favorited")), Direction.PREV, false));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "{ profile(username: \"johnjacob\") { profile { articles(first: 2) { edges { node {"
                + " slug } } } favorites(last: 2, before: \"4000\") { edges { node { slug } } } } }"
                + " }");

    assertThat(context.read("$.data.profile.profile.articles.edges[0].node.slug", String.class))
        .isEqualTo("title-authored");
    assertThat(context.read("$.data.profile.profile.favorites.edges[0].node.slug", String.class))
        .isEqualTo("title-favorited");
  }

  @Test
  void should_fail_profile_articles_without_first_or_last() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"johnjacob\") { profile { articles { edges { cursor } } } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  @Test
  void should_fail_profile_favorites_without_first_or_last() {
    when(profileQueryService.findByUsername(eq("johnjacob"), eq(user)))
        .thenReturn(Optional.of(profileData));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"johnjacob\") { profile { favorites { edges { cursor } } } } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("first 和 last 必须只存在一个");
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_build_article_from_comment_local_context() {
    ArticleData articleData = articleData("commented");
    CommentData commentData =
        new CommentData("comment-id", "comment body", articleData.getId(), TIME, TIME, profileData);
    when(articleQueryService.findById(eq(articleData.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("title-commented");
    assertThat(result.getData().getBody()).isEqualTo("body commented");
    Map<String, Object> localContext = (Map<String, Object>) result.<Object>getLocalContext();
    assertThat(localContext).containsKey(articleData.getSlug());
  }

  @Test
  void should_fail_to_build_article_from_comment_of_missing_article() {
    CommentData commentData =
        new CommentData("comment-id", "comment body", "missing-id", TIME, TIME, profileData);
    when(articleQueryService.findById(eq("missing-id"), eq(user))).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(commentData);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> articleDatafetcher.getCommentArticle(dfe))
        .isInstanceOf(io.spring.api.exception.ResourceNotFoundException.class);
  }

  private CursorPageParameter<DateTime> captureFeedPageParameter() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(any(), captor.capture());
    return captor.getValue();
  }

  private CursorPageParameter<DateTime> captureRecentArticlesPageParameter() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(any(), any(), any(), captor.capture(), any());
    return captor.getValue();
  }

  private ArticleData articleData(String seed) {
    return new ArticleData(
        seed + "-id",
        "title-" + seed,
        "title " + seed,
        "desc " + seed,
        "body " + seed,
        true,
        3,
        TIME,
        TIME,
        Arrays.asList("joda"),
        profileData);
  }
}
