package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
public class FeedDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ProfileQueryService profileQueryService;

  private User victim;

  @BeforeEach
  public void setUp() {
    victim = new User("victim@example.com", "victim", "123", "", "");
    when(profileQueryService.findByUsername(eq("victim"), any()))
        .thenReturn(Optional.of(new ProfileData(victim.getId(), "victim", "", "", false)));
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_not_expose_feed_on_profile() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"victim\") { profile { feed(first: 10) { edges { cursor } } } }"
                + " }");

    assertFalse(result.getErrors().isEmpty());
    assertEquals("ValidationError", result.getErrors().get(0).getErrorType().toString());
    verify(articleQueryService, never()).findUserFeedWithCursor(any(), any());
  }

  @Test
  public void should_reject_anonymous_feed_query() {
    ExecutionResult result = dgsQueryExecutor.execute("{ feed(first: 10) { edges { cursor } } }");

    assertFalse(result.getErrors().isEmpty());
    Map<String, Object> data = result.getData();
    assertNull(data.get("feed"));
    verify(articleQueryService, never()).findUserFeedWithCursor(any(), any());
  }

  @Test
  public void should_return_feed_of_current_user() {
    User current = new User("me@example.com", "me", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));

    ExecutionResult result = dgsQueryExecutor.execute("{ feed(first: 10) { edges { cursor } } }");

    assertTrue(result.getErrors().isEmpty());
    verify(articleQueryService).findUserFeedWithCursor(eq(current), any());
  }
}
