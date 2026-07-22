package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArticleDatafetcherTest extends GraphQLTestBase {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment env;

  private ArticleDatafetcher articleDatafetcher;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    setAnonymous();
  }

  private ArticleData articleData(String slug) {
    return new ArticleData(
        "id-" + slug,
        slug,
        "title",
        "desc",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Collections.singletonList("java"),
        new ProfileData("pid", "author", "bio", "image", false));
  }

  @Test
  public void should_get_feed_with_first() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(articleData("a-slug")), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, new DgsDataFetchingEnvironment(env));

    assertThat(result.getData().getEdges().size(), is(1));
    assertThat(result.getData().getEdges().get(0).getNode().getSlug(), is("a-slug"));
  }

  @Test
  public void should_throw_when_feed_missing_first_and_last() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getFeed(
                null, null, null, null, new DgsDataFetchingEnvironment(env)));
  }

  @Test
  public void should_get_articles_with_first() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.singletonList(articleData("a-slug")), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, null, null, null, new DgsDataFetchingEnvironment(env));

    assertThat(result.getData().getEdges().size(), is(1));
  }

  @Test
  public void should_find_article_by_slug() {
    when(articleQueryService.findBySlug(eq("a-slug"), any()))
        .thenReturn(Optional.of(articleData("a-slug")));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.findArticleBySlug("a-slug");

    assertThat(result.getData().getSlug(), is("a-slug"));
    assertThat(result.getData().getTitle(), is("title"));
  }

  @Test
  public void should_throw_when_article_by_slug_not_found() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }

  @Test
  public void should_get_article_from_local_context() {
    User user = new User("email@test.com", "username", "pass", "", "");
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("java"), user.getId());
    when(env.<io.spring.core.article.Article>getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData("a-slug")));

    DataFetcherResult<io.spring.graphql.types.Article> result = articleDatafetcher.getArticle(env);

    assertThat(result.getData().getSlug(), is("a-slug"));
  }

  @Test
  public void should_throw_when_get_article_not_found() {
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("java"), "author-id");
    when(env.<io.spring.core.article.Article>getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(env));
  }
}
