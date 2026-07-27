package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleDatafetcherTest extends GraphQLTestBase {

  private ArticleData articleData;

  @BeforeEach
  public void setUpFixtures() {
    articleData = TestHelper.articleDataFixture("1", user);
  }

  private CursorPager<ArticleData> pagerOf(ArticleData... data) {
    return new CursorPager<>(Arrays.asList(data), Direction.NEXT, false);
  }

  @Test
  public void should_query_feed_when_authenticated() {
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerOf(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }",
            "data.feed.edges[0].node.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
  }

  @Test
  public void should_reject_feed_without_first_or_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ feed { edges { node { slug } } } }");
    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_query_articles_anonymously() {
    logout();
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(), isNull()))
        .thenReturn(pagerOf(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ articles(first: 10) { edges { node { slug } } } }",
            "data.articles.edges[0].node.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
  }

  @Test
  public void should_reject_articles_without_first_or_last() {
    ExecutionResult result = dgsQueryExecutor.execute("{ articles { edges { node { slug } } } }");
    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_find_article_by_slug_with_author() {
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), any()))
        .thenReturn(java.util.Optional.of(articleData));
    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(
            java.util.Optional.of(new ProfileData(user.getId(), username, "bio", defaultAvatar, false)));

    String author =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ article(slug: \"%s\") { slug title favorited author { username } } }",
                articleData.getSlug()),
            "data.article.author.username");

    Assertions.assertEquals(username, author);
  }

  @Test
  public void should_return_error_when_article_not_found() {
    when(articleQueryService.findBySlug(any(), any())).thenReturn(java.util.Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute("{ article(slug: \"missing\") { slug } }");
    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_query_profile_articles_feed_and_favorites() {
    when(profileQueryService.findByUsername(eq(username), any()))
        .thenReturn(
            java.util.Optional.of(new ProfileData(user.getId(), username, "bio", defaultAvatar, false)));
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerOf(articleData));
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerOf(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "{ profile(username: \"%s\") { profile { username "
                    + "articles(first: 10) { edges { node { slug } } } "
                    + "favorites(first: 10) { edges { node { slug } } } "
                    + "feed(first: 10) { edges { node { slug } } } } } }",
                username),
            "data.profile.profile.articles.edges[0].node.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
  }

  @Test
  public void should_return_empty_edges_when_no_articles() {
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    java.util.List<Object> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ feed(first: 10) { edges { node { slug } } } }", "data.feed.edges");

    Assertions.assertTrue(edges.isEmpty());
  }
}
