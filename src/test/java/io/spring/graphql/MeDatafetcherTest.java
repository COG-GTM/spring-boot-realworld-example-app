package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {

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

  private User authenticate() {
    User user = new User("user@example.com", "user", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    return user;
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  void getMe_returns_null_when_anonymous() {
    anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertNull(meDatafetcher.getMe("Token jwt", dfe));
  }

  @Test
  void getMe_returns_user_with_token_from_authorization_header() {
    User user = authenticate();
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "", "");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-abc", dfe);

    assertEquals(user.getEmail(), result.getData().getEmail());
    assertEquals(user.getUsername(), result.getData().getUsername());
    assertEquals("jwt-abc", result.getData().getToken());
    assertSame(user, result.getLocalContext());
  }

  @Test
  void getMe_throws_not_found_when_user_missing() {
    User user = authenticate();
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);

    assertThrows(ResourceNotFoundException.class, () -> meDatafetcher.getMe("Token jwt-abc", dfe));
  }

  @Test
  void getMe_throws_on_malformed_authorization_header() {
    User user = authenticate();
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "", "");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);

    assertThrows(ArrayIndexOutOfBoundsException.class, () -> meDatafetcher.getMe("malformed", dfe));
  }

  @Test
  void getUserPayloadUser_builds_user_with_generated_token() {
    User user = new User("payload@example.com", "payloaduser", "pw", "", "");
    when(jwtService.toToken(any(User.class))).thenReturn("generated-token");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<User>getLocalContext()).thenReturn(user);

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertEquals("payload@example.com", result.getData().getEmail());
    assertEquals("payloaduser", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
    assertSame(user, result.getLocalContext());
  }
}
