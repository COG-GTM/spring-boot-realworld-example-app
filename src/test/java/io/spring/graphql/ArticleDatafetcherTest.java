package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("a@b.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData createArticleData(String id, String slug) {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("pid", "testuser", "bio", "img", false);
    return new ArticleData(id, slug, "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
  }

  private DgsDataFetchingEnvironment createDgsEnv() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    return new DgsDataFetchingEnvironment(delegate);
  }

  private DgsDataFetchingEnvironment createDgsEnvWithSource(Object source) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(source);
    return new DgsDataFetchingEnvironment(delegate);
  }

  @Test
  public void should_get_feed_with_first() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getFeed(10, null, null, null, dfe);
    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_feed_with_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getFeed(null, null, 10, null, dfe);
    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_throw_when_feed_missing_first_and_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    assertThrows(IllegalArgumentException.class, () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  public void should_get_feed_with_empty_results() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    CursorPager<ArticleData> pager = new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getFeed(10, null, null, null, dfe);
    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  public void should_get_articles_with_first() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(10, null, null, null, "author", "fav", "tag", dfe);
    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_articles_with_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(null, null, 10, null, null, null, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_articles_missing_first_and_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    assertThrows(IllegalArgumentException.class, () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  public void should_find_article_by_slug() {
    ArticleData articleData = createArticleData("a1", "slug1");
    when(articleQueryService.findBySlug(eq("slug1"), any())).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug1");
    assertNotNull(result);
    assertEquals("slug1", result.getData().getSlug());
  }

  @Test
  public void should_throw_when_article_not_found_by_slug() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }

  @Test
  public void should_get_article_from_payload() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article coreArticle = new io.spring.core.article.Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    ArticleData articleData = createArticleData(coreArticle.getId(), "title");
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);
    assertNotNull(result);
  }

  @Test
  public void should_get_comment_article() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("pid", "testuser", "bio", "img", false);
    CommentData commentData = new CommentData("c1", "body", "a1", now, now, profile);
    when(dfe.getLocalContext()).thenReturn(commentData);
    ArticleData articleData = createArticleData("a1", "slug1");
    when(articleQueryService.findById(eq("a1"), any())).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);
    assertNotNull(result);
  }

  @Test
  public void should_get_user_feed() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userFeed(10, null, null, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_get_user_feed_with_last() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    CursorPager<ArticleData> pager = new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userFeed(null, null, 5, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_user_feed_missing_first_and_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    assertThrows(IllegalArgumentException.class, () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  public void should_get_user_favorites() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("testuser"), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userFavorites(10, null, null, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_get_user_favorites_with_last() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    CursorPager<ArticleData> pager = new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("testuser"), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userFavorites(null, null, 5, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_user_favorites_missing_first_and_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    assertThrows(IllegalArgumentException.class, () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  public void should_get_user_articles() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    ArticleData articleData = createArticleData("a1", "slug1");
    CursorPager<ArticleData> pager = new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userArticles(10, null, null, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_get_user_articles_with_last() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    DgsDataFetchingEnvironment dfe = createDgsEnvWithSource(profile);
    CursorPager<ArticleData> pager = new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.userArticles(null, null, 5, null, dfe);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_user_articles_missing_first_and_last() {
    DgsDataFetchingEnvironment dfe = createDgsEnv();
    assertThrows(IllegalArgumentException.class, () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }
}
