package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
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
class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;
  private User user;
  private UserData userData;

  @BeforeEach
  void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("test@test.com", "testuser", "password", "bio", "http://image.url");
    userData = new UserData(user.getId(), "test@test.com", "testuser", "bio", "http://image.url");
    SecurityContextHolder.clearContext();
  }

  // --- getMe tests ---

  @Test
  void getMe_authenticated_returnsUser() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token mytoken123", dataFetchingEnvironment);

    assertNotNull(result);
    io.spring.graphql.types.User userResult = result.getData();
    assertEquals("test@test.com", userResult.getEmail());
    assertEquals("testuser", userResult.getUsername());
    assertEquals("mytoken123", userResult.getToken());
  }

  @Test
  void getMe_authenticated_setsLocalContextWithUser() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token mytoken123", dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void getMe_anonymous_returnsNull() {
    AnonymousAuthenticationToken anonToken =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonToken);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token sometoken", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_nullPrincipal_returnsNull() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token sometoken", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_userNotFoundInQueryService_throwsResourceNotFound() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token mytoken123", dataFetchingEnvironment));
  }

  @Test
  void getMe_splitsTokenFromAuthorizationHeader() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc.def.ghi", dataFetchingEnvironment);

    assertEquals("abc.def.ghi", result.getData().getToken());
  }

  // --- getUserPayloadUser tests ---

  @Test
  void getUserPayloadUser_returnsUserFromLocalContext() {
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-jwt-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertNotNull(result);
    io.spring.graphql.types.User userResult = result.getData();
    assertEquals("test@test.com", userResult.getEmail());
    assertEquals("testuser", userResult.getUsername());
    assertEquals("generated-jwt-token", userResult.getToken());
  }

  @Test
  void getUserPayloadUser_setsLocalContextWithUser() {
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-jwt-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertEquals(user, result.getLocalContext());
  }
}
