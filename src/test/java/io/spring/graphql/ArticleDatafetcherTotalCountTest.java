package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTotalCountTest {
  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private User currentUser;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    currentUser = new User("current@example.com", "current", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DgsDataFetchingEnvironment env(boolean totalCountSelected, Object source) {
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    DataFetchingFieldSelectionSet selectionSet = mock(DataFetchingFieldSelectionSet.class);
    when(selectionSet.contains(DgsConstants.ARTICLESCONNECTION.TotalCount))
        .thenReturn(totalCountSelected);
    when(inner.getSelectionSet()).thenReturn(selectionSet);
    when(inner.getSource()).thenReturn(source);
    return new DgsDataFetchingEnvironment(inner);
  }

  @Test
  public void should_return_filtered_total_count_for_articles_when_selected() {
    when(articleQueryService.countRecentArticles("java", "jake", "jane")).thenReturn(42);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "jake", "jane", "java", env(true, null));

    assertEquals(42, result.getData().getTotalCount());
  }

  @Test
  public void should_skip_count_query_when_total_count_not_selected() {
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, env(false, null));

    assertNull(result.getData().getTotalCount());
    verify(articleQueryService, never()).countRecentArticles(any(), any(), any());
  }

  @Test
  public void should_return_feed_total_count_for_current_user() {
    when(articleQueryService.countUserFeed(currentUser)).thenReturn(3);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, env(true, null));

    assertEquals(3, result.getData().getTotalCount());
  }

  @Test
  public void should_return_profile_articles_and_favorites_total_count() {
    Profile profile = Profile.newBuilder().username("jake").build();
    when(articleQueryService.countRecentArticles(isNull(), eq("jake"), isNull())).thenReturn(5);
    when(articleQueryService.countRecentArticles(isNull(), isNull(), eq("jake"))).thenReturn(7);

    assertEquals(
        5,
        articleDatafetcher
            .userArticles(10, null, null, null, env(true, profile))
            .getData()
            .getTotalCount());
    assertEquals(
        7,
        articleDatafetcher
            .userFavorites(10, null, null, null, env(true, profile))
            .getData()
            .getTotalCount());
  }

  @Test
  public void should_return_profile_feed_total_count() {
    User jake = new User("jake@example.com", "jake", "pass", "", "");
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(jake));
    when(articleQueryService.countUserFeed(jake)).thenReturn(2);

    assertEquals(
        2,
        articleDatafetcher
            .userFeed(
                10, null, null, null, env(true, Profile.newBuilder().username("jake").build()))
            .getData()
            .getTotalCount());
  }
}
