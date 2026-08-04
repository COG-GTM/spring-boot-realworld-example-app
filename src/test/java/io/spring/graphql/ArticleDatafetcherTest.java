package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import io.spring.graphql.types.Article;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class,
      ProfileDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
class ArticleDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private UserRepository userRepository;
  // Collaborator for ProfileDatafetcher, imported to resolve the nested author.
  @MockBean private ProfileQueryService profileQueryService;

  private final User author = new User("a@example.com", "author", "123", "author bio", "a.png");

  private void stubAuthorProfile() {
    when(profileQueryService.findByUsername(eq("author"), any()))
        .thenReturn(
            Optional.of(new ProfileData(author.getId(), "author", "author bio", "a.png", false)));
  }

  @Test
  void should_find_article_by_slug_with_nested_author() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("1", author);
    when(articleQueryService.findBySlug(eq(data.getSlug()), any())).thenReturn(Optional.of(data));
    stubAuthorProfile();

    String query =
        "{ article(slug: \""
            + data.getSlug()
            + "\") { slug title body favorited favoritesCount author { username bio } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.read("data.article.slug", String.class)).isEqualTo(data.getSlug());
    assertThat(context.read("data.article.title", String.class)).isEqualTo(data.getTitle());
    assertThat(context.read("data.article.body", String.class)).isEqualTo(data.getBody());
    assertThat(context.read("data.article.favorited", Boolean.class)).isFalse();
    assertThat(context.read("data.article.author.username", String.class)).isEqualTo("author");
    assertThat(context.read("data.article.author.bio", String.class)).isEqualTo("author bio");
    verify(articleQueryService).findBySlug(eq(data.getSlug()), eq(null));
  }

  @Test
  void should_error_when_article_not_found_by_slug() {
    anonymous();
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    ExecutionResult result = dgsQueryExecutor.execute("{ article(slug: \"missing\") { slug } }");

    assertSingleErrorFrom(result, ResourceNotFoundException.class);
  }

  @Test
  void should_return_articles_connection_with_nested_author() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("2", author);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(data), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);
    stubAuthorProfile();

    String query =
        "{ articles(first: 10) { edges { node { slug author { username } } }"
            + " pageInfo { hasNextPage hasPreviousPage } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.<List<String>>read("data.articles.edges[*].node.slug"))
        .containsExactly(data.getSlug());
    assertThat(context.<List<String>>read("data.articles.edges[*].node.author.username"))
        .containsExactly("author");
    assertThat(context.read("data.articles.pageInfo.hasNextPage", Boolean.class)).isFalse();
    assertThat(context.read("data.articles.pageInfo.hasPreviousPage", Boolean.class)).isFalse();
  }

  @Test
  void should_error_articles_when_neither_first_nor_last() {
    anonymous();

    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { node { slug } } } }");

    assertSingleErrorFrom(result, IllegalArgumentException.class);
  }

  @Test
  void should_return_articles_backwards_when_paging_with_last_and_before() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("2b", author);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(data), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    String query =
        "{ articles(last: 5, before: \""
            + data.getCursor()
            + "\", withTag: \"java\", authoredBy: \"author\", favoritedBy: \"fan\")"
            + " { edges { cursor node { slug } } pageInfo { hasPreviousPage hasNextPage } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.read("data.articles.pageInfo.hasPreviousPage", Boolean.class)).isTrue();
    assertThat(context.read("data.articles.pageInfo.hasNextPage", Boolean.class)).isFalse();
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), eq("author"), eq("fan"), captor.capture(), eq(null));
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
  }

  @Test
  void should_return_articles_authored_by_profile() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("4", author);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("author"), eq(null), any(), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(data), Direction.NEXT, false));
    stubAuthorProfile();

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"author\") { profile { articles(first: 10)"
                + " { edges { node { slug } } } } } }",
            "data.profile.profile.articles.edges[*].node.slug");

    assertThat(slugs).containsExactly(data.getSlug());
  }

  @Test
  void should_return_articles_favorited_by_profile() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("5", author);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("author"), any(), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(data), Direction.NEXT, false));
    stubAuthorProfile();

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"author\") { profile { favorites(first: 10)"
                + " { edges { node { slug } } } } } }",
            "data.profile.profile.favorites.edges[*].node.slug");

    assertThat(slugs).containsExactly(data.getSlug());
  }

  @Test
  void should_return_feed_of_a_profile() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("6", author);
    when(userRepository.findByUsername(eq("author"))).thenReturn(Optional.of(author));
    when(articleQueryService.findUserFeedWithCursor(eq(author), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(data), Direction.NEXT, false));
    stubAuthorProfile();

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"author\") { profile { feed(first: 10)"
                + " { edges { node { slug } } } } } }",
            "data.profile.profile.feed.edges[*].node.slug");

    assertThat(slugs).containsExactly(data.getSlug());
  }

  @Test
  void should_error_profile_feed_when_user_missing() {
    anonymous();
    when(userRepository.findByUsername(eq("author"))).thenReturn(Optional.empty());
    stubAuthorProfile();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"author\") { profile { feed(first: 10)"
                + " { edges { node { slug } } } } } }");

    assertSingleErrorFrom(result, ResourceNotFoundException.class);
  }

  @Test
  void should_page_profile_articles_favorites_and_feed_backwards() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("8", author);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(data), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);
    when(articleQueryService.findUserFeedWithCursor(eq(author), any())).thenReturn(pager);
    when(userRepository.findByUsername(eq("author"))).thenReturn(Optional.of(author));
    stubAuthorProfile();

    String cursor = data.getCursor().toString();
    for (String field : new String[] {"articles", "favorites", "feed"}) {
      String query =
          "{ profile(username: \"author\") { profile { "
              + field
              + "(last: 2, before: \""
              + cursor
              + "\") { edges { node { slug } } pageInfo { hasPreviousPage } } } } }";

      DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

      assertThat(
              context.<List<String>>read("data.profile.profile." + field + ".edges[*].node.slug"))
          .as(field)
          .containsExactly(data.getSlug());
      assertThat(
              context.read(
                  "data.profile.profile." + field + ".pageInfo.hasPreviousPage", Boolean.class))
          .as(field)
          .isTrue();
    }
  }

  @Test
  void should_error_when_pagination_arguments_are_missing() {
    anonymous();
    stubAuthorProfile();

    assertSingleErrorFrom(
        dgsQueryExecutor.execute("{ feed { edges { node { slug } } } }"),
        IllegalArgumentException.class);
    for (String field : new String[] {"articles", "favorites", "feed"}) {
      ExecutionResult result =
          dgsQueryExecutor.execute(
              "{ profile(username: \"author\") { profile { "
                  + field
                  + " { edges { node { slug } } } } } }");
      assertSingleErrorFrom(result, IllegalArgumentException.class);
    }
  }

  @Test
  void should_page_feed_backwards_for_current_user() {
    authenticate(author);
    ArticleData data = TestHelper.articleDataFixture("9", author);
    when(articleQueryService.findUserFeedWithCursor(eq(author), any()))
        .thenReturn(new CursorPager<>(Collections.singletonList(data), Direction.PREV, true));

    String query =
        "{ feed(last: 2, before: \""
            + data.getCursor()
            + "\") { edges { node { slug } } pageInfo { hasPreviousPage } } }";

    Boolean hasPrevious =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.pageInfo.hasPreviousPage");

    assertThat(hasPrevious).isTrue();
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(author), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis())
        .isEqualTo(data.getUpdatedAt().getMillis());
  }

  @Test
  void should_return_null_cursors_when_no_articles_match() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    String query =
        "{ articles(first: 10) { edges { node { slug } }"
            + " pageInfo { startCursor endCursor hasNextPage } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.<List<Object>>read("data.articles.edges")).isEmpty();
    assertThat(context.read("data.articles.pageInfo.startCursor", String.class)).isNull();
    assertThat(context.read("data.articles.pageInfo.endCursor", String.class)).isNull();
    assertThat(context.read("data.articles.pageInfo.hasNextPage", Boolean.class)).isFalse();
  }

  @Test
  void should_resolve_article_of_a_comment_from_local_context() {
    anonymous();
    ArticleData data = TestHelper.articleDataFixture("7", author);
    when(articleQueryService.findById(eq("article-id"), eq(null))).thenReturn(Optional.of(data));
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData(author.getId(), "author", "", "", false));

    ArticleDatafetcher datafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    DataFetcherResult<Article> result =
        datafetcher.getCommentArticle(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment().localContext(comment).build());

    assertThat(result.getData().getSlug()).isEqualTo(data.getSlug());
    assertThat(result.getData().getTitle()).isEqualTo(data.getTitle());
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsKey(data.getSlug());
  }

  @Test
  void should_return_feed_for_current_user() {
    authenticate(author);
    ArticleData data = TestHelper.articleDataFixture("3", author);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(data), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(eq(author), any())).thenReturn(pager);

    String query = "{ feed(first: 5) { edges { node { slug } } pageInfo { hasNextPage } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.<List<String>>read("data.feed.edges[*].node.slug"))
        .containsExactly(data.getSlug());
    assertThat(context.read("data.feed.pageInfo.hasNextPage", Boolean.class)).isTrue();
    verify(articleQueryService).findUserFeedWithCursor(eq(author), any());
  }
}
