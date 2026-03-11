package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleDatafetcher.class})
public class ArticleDatafetcherTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_get_article_by_slug() {
    String slug = "test-article";
    Article article = new Article("Test Article", "Description", "Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleQueryService.findBySlug(eq(slug), any())).thenReturn(Optional.of(articleData));

    String query =
        "query { article(slug: \"" + slug + "\") { slug title description body favorited favoritesCount tagList } }";

    String resultSlug = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.slug");
    String resultTitle = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.title");
    String resultBody = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article.body");

    assertThat(resultSlug).isEqualTo(articleData.getSlug());
    assertThat(resultTitle).isEqualTo(articleData.getTitle());
    assertThat(resultBody).isEqualTo(articleData.getBody());
  }

  @Test
  public void should_get_articles_with_pagination() {
    ArticleData articleData1 = TestHelper.articleDataFixture("1", user);
    ArticleData articleData2 = TestHelper.articleDataFixture("2", user);
    List<ArticleData> articles = Arrays.asList(articleData1, articleData2);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, true);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");
    Boolean hasNextPage =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasNextPage");
    Boolean hasPreviousPage =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.pageInfo.hasPreviousPage");

    assertThat(edges).hasSize(2);
    assertThat(hasNextPage).isTrue();
    assertThat(hasPreviousPage).isFalse();
  }

  @Test
  public void should_get_articles_filtered_by_tag() {
    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, withTag: \"java\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_articles_filtered_by_author() {
    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("johnjacob"), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, authoredBy: \"johnjacob\") { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_feed_for_authenticated_user() {
    setAuthenticatedUser(user);

    ArticleData articleData = TestHelper.articleDataFixture("1", user);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(any(User.class), any()))
        .thenReturn(cursorPager);

    String query =
        "query { feed(first: 10) { edges { node { slug title } } pageInfo { hasNextPage } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed.edges");

    assertThat(edges).hasSize(1);
  }

  @Test
  public void should_get_empty_articles_when_none_exist() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10) { edges { node { slug } } } }";

    List<Map<String, Object>> edges =
        dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles.edges");

    assertThat(edges).isEmpty();
  }
}
