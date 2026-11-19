package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
  private DataFetchingEnvironment dfe;
  private User user;

  @BeforeEach
  public void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    dfe = mock(DataFetchingEnvironment.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("alice@example.com", "alice", "pw", "bio", "img");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_return_me_with_token_from_authorization_header() {
    authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(
            Optional.of(new UserData(user.getId(), "alice@example.com", "alice", "bio", "img")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc.def.ghi", dfe);

    assertEquals("alice@example.com", result.getData().getEmail());
    assertEquals("alice", result.getData().getUsername());
    assertEquals("abc.def.ghi", result.getData().getToken());
    assertSame(user, result.getLocalContext());
    verify(userQueryService).findById(user.getId());
  }

  @Test
  public void should_throw_not_found_when_user_data_missing() {
    authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> meDatafetcher.getMe("Token abc.def.ghi", dfe));
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    anonymous();

    assertNull(meDatafetcher.getMe("Token whatever", dfe));
    verifyNoInteractions(userQueryService);
  }

  @Test
  public void should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertNull(meDatafetcher.getMe("Token whatever", dfe));
    verifyNoInteractions(userQueryService);
  }

  @Test
  public void should_build_user_payload_user_from_local_context() {
    when(dfe.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("new-token");

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertEquals("alice@example.com", result.getData().getEmail());
    assertEquals("alice", result.getData().getUsername());
    assertEquals("new-token", result.getData().getToken());
    assertSame(user, result.getLocalContext());
    verify(jwtService).toToken(user);
  }
}
