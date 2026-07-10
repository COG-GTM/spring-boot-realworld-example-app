package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArticleDatafetcherTest extends DgsGraphQLTestBase {

  private CursorPager<ArticleData> onePage() {
    return new CursorPager<>(
        Collections.singletonList(articleData("hello-world", user)), Direction.NEXT, false);
  }

  @Test
  void should_query_feed() {
    setAuthenticatedUser(user);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(onePage());

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { edges { cursor node { slug title body } } pageInfo { hasNextPage"
                + " hasPreviousPage } } }",
            "data.feed.edges[*].node.slug");

    assertEquals(Collections.singletonList("hello-world"), slugs);
  }

  @Test
  void should_query_feed_backwards() {
    setAuthenticatedUser(user);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(onePage());

    Boolean hasPrevious =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(last: 10) { edges { node { slug } } pageInfo { hasPreviousPage } } }",
            "data.feed.pageInfo.hasPreviousPage");

    assertFalse(hasPrevious);
  }

  @Test
  void should_error_when_feed_missing_first_and_last() {
    setAuthenticatedUser(user);

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ feed { edges { node { slug } } } }", "data.feed"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_query_articles_with_filters() {
    setAnonymous();
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(onePage());

    List<String> slugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ articles(first: 5, withTag: \"java\", authoredBy: \"johnjacob\") { edges { node {"
                + " slug title } } pageInfo { hasNextPage } } }",
            "data.articles.edges[*].node.slug");

    assertEquals(Collections.singletonList("hello-world"), slugs);
  }

  @Test
  void should_query_articles_backwards() {
    setAnonymous();
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(onePage());

    Boolean hasNext =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ articles(last: 5) { edges { node { slug } } pageInfo { hasNextPage } } }",
            "data.articles.pageInfo.hasNextPage");

    assertFalse(hasNext);
  }

  @Test
  void should_find_article_by_slug() {
    setAnonymous();
    ArticleData articleData = articleData("hello-world", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));

    String title =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \"hello-world\") { slug title body description favorited favoritesCount"
                + " tagList createdAt updatedAt } }",
            "data.article.title");

    assertEquals("title hello-world", title);
  }

  @Test
  void should_error_when_article_not_found() {
    setAnonymous();
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ article(slug: \"missing\") { slug } }", "data.article"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_resolve_profile_articles_and_feed_and_favorites() {
    setAuthenticatedUser(user);
    when(profileQueryService.findByUsername(any(), any()))
        .thenReturn(Optional.of(profileData(user)));
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(onePage());
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(onePage());

    List<String> articleSlugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"johnjacob\") { profile { username articles(first: 3) { edges {"
                + " node { slug } } } favorites(first: 3) { edges { node { slug } } } feed(first: 3)"
                + " { edges { node { slug } } } } } }",
            "data.profile.profile.articles.edges[*].node.slug");

    assertEquals(Collections.singletonList("hello-world"), articleSlugs);
  }

  @Test
  void should_resolve_profile_articles_backwards() {
    setAnonymous();
    when(profileQueryService.findByUsername(any(), any()))
        .thenReturn(Optional.of(profileData(user)));
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(onePage());
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(onePage());

    List<String> favoriteSlugs =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ profile(username: \"johnjacob\") { profile { articles(last: 3) { edges { node { slug"
                + " } } } favorites(last: 3) { edges { node { slug } } } feed(last: 3) { edges {"
                + " node { slug } } } } } }",
            "data.profile.profile.favorites.edges[*].node.slug");

    assertEquals(Collections.singletonList("hello-world"), favoriteSlugs);
  }

  @Test
  void should_error_when_profile_feed_missing_pagination() {
    setAnonymous();
    when(profileQueryService.findByUsername(any(), any()))
        .thenReturn(Optional.of(profileData(user)));
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ profile(username: \"johnjacob\") { profile { feed { edges { node { slug } } }"
                        + " } } }",
                    "data.profile.profile.feed"));

    assertTrue(error.getErrors().size() > 0);
  }

  @Test
  void should_error_when_profile_feed_target_missing() {
    setAnonymous();
    when(profileQueryService.findByUsername(any(), any()))
        .thenReturn(Optional.of(profileData(user)));
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ profile(username: \"ghost\") { profile { feed(first: 3) { edges { node { slug"
                        + " } } } } } }",
                    "data.profile.profile.feed"));

    assertFalse(error.getErrors().isEmpty());
  }
}
