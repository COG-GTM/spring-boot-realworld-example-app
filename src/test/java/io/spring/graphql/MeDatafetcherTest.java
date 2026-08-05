package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;
  private User user;
  private UserData userData;

  @BeforeEach
  public void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                currentUser, null, AuthorityUtils.NO_AUTHORITIES));
  }

  @Test
  public void should_return_current_user_with_token_from_authorization_header() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class));

    assertEquals("john@jacob.com", result.getData().getEmail());
    assertEquals("johnjacob", result.getData().getUsername());
    assertEquals("jwt-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_return_null_when_user_is_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertNull(meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class)));
  }

  @Test
  public void should_return_null_when_principal_is_null() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertNull(meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class)));
  }

  @Test
  public void should_throw_not_found_when_current_user_is_missing_in_read_model() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class)));
  }

  @Test
  public void should_build_user_payload_from_local_context_with_freshly_signed_token() {
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    doReturn(user).when(dataFetchingEnvironment).getLocalContext();
    when(jwtService.toToken(eq(user))).thenReturn("new-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertEquals("john@jacob.com", result.getData().getEmail());
    assertEquals("johnjacob", result.getData().getUsername());
    assertEquals("new-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_fail_when_authorization_header_has_no_scheme_prefix() {
    authenticate(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> meDatafetcher.getMe("jwt-token", mock(DataFetchingEnvironment.class)));
  }
}
