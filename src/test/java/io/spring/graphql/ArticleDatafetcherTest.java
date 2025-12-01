package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock
  private ArticleQueryService articleQueryService;

  @Mock
  private UserRepository userRepository;

  private ArticleDatafetcher articleDatafetcher;

  private User user;
  private ArticleData articleData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    
    DateTime now = new DateTime();
    articleData = new ArticleData(
        "article-id",
        "test-article",
        "Test Article",
        "Test Description",
        "Test Body",
        false,
        5,
        now,
        now,
        Arrays.asList("java", "spring"),
        profileData
    );
    
    setAnonymousAuthentication();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", 
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  void shouldGetArticleBySlug() {
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getSlug()).isEqualTo("test-article");
    assertThat(result.getData().getTitle()).isEqualTo("Test Article");
    assertThat(result.getData().getDescription()).isEqualTo("Test Description");
    assertThat(result.getData().getBody()).isEqualTo("Test Body");
    assertThat(result.getData().getFavorited()).isFalse();
    assertThat(result.getData().getFavoritesCount()).isEqualTo(5);
    assertThat(result.getData().getTagList()).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  void shouldThrowExceptionWhenArticleNotFound() {
    when(articleQueryService.findBySlug(eq("non-existent"), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.findArticleBySlug("non-existent"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldGetArticlesWithFirstPagination() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        10, null, null, null, null, null, null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
  }

  @Test
  void shouldGetArticlesFilteredByTag() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(eq("java"), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        10, null, null, null, null, null, "java", null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void shouldGetArticlesFilteredByAuthor() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), eq("testuser"), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        10, null, null, null, "testuser", null, null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void shouldGetArticlesFilteredByFavoritedBy() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), eq("testuser"), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        10, null, null, null, null, "testuser", null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void shouldGetEmptyArticlesConnection() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        new ArrayList<>(),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        10, null, null, null, null, null, null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void shouldGetFeedForAuthenticatedUser() {
    setAuthenticatedUser(user);
    
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        false
    );
    
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any(CursorPageParameter.class)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getFeed(
        10, null, null, null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void shouldGetArticlesWithLastPagination() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.PREV,
        false
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        null, null, 10, null, null, null, null, null);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void shouldGetArticlesWithHasNextPage() {
    CursorPager<ArticleData> cursorPager = new CursorPager<>(
        Arrays.asList(articleData),
        Direction.NEXT,
        true
    );
    
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result = articleDatafetcher.getArticles(
        1, null, null, null, null, null, null, null);

    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenNeitherFirstNorLastProvided() {
    assertThatThrownBy(() -> articleDatafetcher.getArticles(
        null, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowExceptionWhenFeedNeitherFirstNorLastProvided() {
    assertThatThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
