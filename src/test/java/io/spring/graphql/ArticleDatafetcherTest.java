package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.spring.application.ArticleQueryService;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {
  private ArticleQueryService articleQueryService;
  private ArticleDatafetcher articleDatafetcher;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, mock(UserRepository.class));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_require_authentication_for_feed_query() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    Assertions.assertThrows(
        AuthenticationException.class,
        () -> articleDatafetcher.getFeed(10, null, null, null, null));
    verify(articleQueryService, never()).findUserFeedWithCursor(any(), any());
  }

  @Test
  public void should_require_authentication_for_feed_query_without_security_context() {
    SecurityContextHolder.clearContext();
    Assertions.assertThrows(
        AuthenticationException.class,
        () -> articleDatafetcher.getFeed(10, null, null, null, null));
  }
}
