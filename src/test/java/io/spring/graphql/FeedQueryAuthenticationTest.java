package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.core.user.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class FeedQueryAuthenticationTest {

  private static final String FEED_QUERY = "{ feed(first: 10) { edges { cursor } } }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @BeforeEach
  public void setUp() {
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_unauthenticated_error_for_anonymous_feed_query() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertUnauthenticated(dgsQueryExecutor.execute(FEED_QUERY));
  }

  @Test
  public void should_return_unauthenticated_error_without_security_context() {
    SecurityContextHolder.clearContext();

    assertUnauthenticated(dgsQueryExecutor.execute(FEED_QUERY));
  }

  @Test
  public void should_check_authentication_before_pagination_arguments() {
    SecurityContextHolder.clearContext();

    assertUnauthenticated(dgsQueryExecutor.execute("{ feed { edges { cursor } } }"));
  }

  @Test
  public void should_return_feed_of_current_user() {
    User current = new User("me@example.com", "me", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));

    ExecutionResult result = dgsQueryExecutor.execute(FEED_QUERY);

    assertTrue(result.getErrors().isEmpty());
    verify(articleQueryService).findUserFeedWithCursor(eq(current), any());
  }

  private void assertUnauthenticated(ExecutionResult result) {
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("UNAUTHENTICATED", String.valueOf(error.getExtensions().get("errorType")));
    assertEquals("Authentication required", error.getMessage());
    assertNull(((Map<?, ?>) result.getData()).get("feed"));
    verify(articleQueryService, never()).findUserFeedWithCursor(any(), any());
  }
}
