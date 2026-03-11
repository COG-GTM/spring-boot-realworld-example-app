package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.Tag;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ArticleDatafetcherTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_get_article_by_slug() {
    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java", "spring"), user.getId());
    ArticleData articleData = createArticleData(article);

    when(articleQueryService.findBySlug(eq(article.getSlug()), any())).thenReturn(Optional.of(articleData));

    String query = "query { article(slug: \"" + article.getSlug() + "\") { slug title description body tagList } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.article");

    assertThat(result).isNotNull();
    assertThat(result.get("slug")).isEqualTo(article.getSlug());
    assertThat(result.get("title")).isEqualTo(article.getTitle());
    assertThat(result.get("description")).isEqualTo(article.getDescription());
    assertThat(result.get("body")).isEqualTo(article.getBody());
  }

  @Test
  public void should_return_error_for_nonexistent_article() {
    when(articleQueryService.findBySlug(any(), any())).thenReturn(Optional.empty());

    String query = "query { article(slug: \"nonexistent\") { slug title } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_get_articles_with_first_pagination() {
    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData(article);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");

    assertThat(result).isNotNull();
    assertThat(result.get("edges")).isNotNull();
    assertThat(result.get("pageInfo")).isNotNull();
  }

  @Test
  public void should_get_articles_with_last_pagination() {
    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData(article);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(last: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_fail_articles_query_without_first_or_last() {
    String query = "query { articles { edges { node { slug } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_get_articles_filtered_by_author() {
    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData(article);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), eq(username), any(), any(), any()))
        .thenReturn(cursorPager);

    String query =
        "query { articles(first: 10, authoredBy: \"" + username + "\") { edges { node { slug title } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_get_articles_filtered_by_tag() {
    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData(article);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    String query = "query { articles(first: 10, withTag: \"java\") { edges { node { slug title tagList } } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.articles");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_get_feed_when_authenticated() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData(article);
    List<ArticleData> articles = Collections.singletonList(articleData);

    CursorPager<ArticleData> cursorPager = new CursorPager<>(articles, Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(cursorPager);

    String query =
        "query { feed(first: 10) { edges { cursor node { slug title } } pageInfo { hasNextPage hasPreviousPage } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.feed");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_fail_feed_query_without_first_or_last() {
    setAuthenticatedUser(user);

    String query = "query { feed { edges { node { slug } } } }";

    var result = dgsQueryExecutor.execute(query);

    assertThat(result.getErrors()).isNotEmpty();
  }

  private ArticleData createArticleData(Article article) {
    DateTime now = new DateTime();
    List<String> tagList = article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    return new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        now,
        now,
        tagList,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }
}
