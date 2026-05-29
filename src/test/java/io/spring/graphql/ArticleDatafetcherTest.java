package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import java.util.ArrayList;
import java.util.Collections;
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
    user = new User("test@test.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_get_feed_with_first() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe =
        mock(com.netflix.graphql.dgs.DgsDataFetchingEnvironment.class);

    var result = articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void should_get_feed_with_last() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe =
        mock(com.netflix.graphql.dgs.DgsDataFetchingEnvironment.class);

    var result = articleDatafetcher.getFeed(null, null, 10, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_first_and_last_both_null() {
    com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe =
        mock(com.netflix.graphql.dgs.DgsDataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class, () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void should_get_articles_with_first() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), CursorPager.Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe =
        mock(com.netflix.graphql.dgs.DgsDataFetchingEnvironment.class);

    var result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result);
  }
}
