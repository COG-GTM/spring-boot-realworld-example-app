package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;

  @BeforeEach
  void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_current_user_when_authenticated() {
    io.spring.core.user.User user =
        new io.spring.core.user.User("me@example.com", "meuser", "pass", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    UserData userData = new UserData(user.getId(), "me@example.com", "meuser", "bio", "image");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer some-token", mock(DataFetchingEnvironment.class));

    io.spring.graphql.types.User data = result.getData();
    assertEquals("me@example.com", data.getEmail());
    assertEquals("meuser", data.getUsername());
    assertEquals("some-token", data.getToken());
    assertSame(user, result.getLocalContext());
  }

  @Test
  void should_return_null_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer some-token", mock(DataFetchingEnvironment.class));

    assertNull(result);
  }

  @Test
  void should_build_user_payload_from_local_context() {
    io.spring.core.user.User user =
        new io.spring.core.user.User("payload@example.com", "payloaduser", "pass", "bio", "image");
    when(jwtService.toToken(any())).thenReturn("tok");

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.user.User>getLocalContext()).thenReturn(user);

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    io.spring.graphql.types.User data = result.getData();
    assertEquals("payload@example.com", data.getEmail());
    assertEquals("payloaduser", data.getUsername());
    assertEquals("tok", data.getToken());
    assertSame(user, result.getLocalContext());
  }
}
