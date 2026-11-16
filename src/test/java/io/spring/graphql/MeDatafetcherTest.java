package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {
  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_current_user_with_request_token_when_authenticated() {
    authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), user.getEmail(), "johnjacob", "", "")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc.def.ghi", emptyEnv());

    assertEquals("john@jacob.com", result.getData().getEmail());
    assertEquals("johnjacob", result.getData().getUsername());
    assertEquals("abc.def.ghi", result.getData().getToken());
    assertSame(user, result.getLocalContext());
    verifyNoInteractions(jwtService);
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertNull(meDatafetcher.getMe(null, emptyEnv()));
    verifyNoInteractions(userQueryService);
  }

  @Test
  public void should_throw_not_found_when_authenticated_user_no_longer_exists() {
    authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> meDatafetcher.getMe("Token abc", emptyEnv()));
  }

  @Test
  public void should_build_user_payload_user_with_fresh_token() {
    when(jwtService.toToken(user)).thenReturn("fresh-token");
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment().localContext(user).build();

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(env);

    assertEquals("john@jacob.com", result.getData().getEmail());
    assertEquals("johnjacob", result.getData().getUsername());
    assertEquals("fresh-token", result.getData().getToken());
    assertSame(user, result.getLocalContext());
  }

  private static void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private static DataFetchingEnvironment emptyEnv() {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();
  }
}
