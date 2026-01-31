package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;
  @Mock private SecurityContext securityContext;

  private MeDatafetcher meDatafetcher;
  private User user;
  private UserData userData;

  @BeforeEach
  void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
  }

  @Test
  void getMe_withAuthenticatedUser_returnsUser() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(userData.getEmail(), result.getData().getEmail());
    assertEquals(userData.getUsername(), result.getData().getUsername());
    assertEquals("token123", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void getMe_withAnonymousAuthentication_returnsNull() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    when(securityContext.getAuthentication()).thenReturn(anonymousToken);
    SecurityContextHolder.setContext(securityContext);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_withNullPrincipal_returnsNull() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_userNotFound_throwsResourceNotFoundException() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment));
  }

  @Test
  void getUserPayloadUser_returnsUser() {
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user.getEmail(), result.getData().getEmail());
    assertEquals(user.getUsername(), result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void getUserPayloadUser_withDifferentUser_returnsCorrectUser() {
    User anotherUser = new User("other@test.com", "otheruser", "password", "other bio", "other image");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(anotherUser);
    when(jwtService.toToken(anotherUser)).thenReturn("another-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals(anotherUser.getEmail(), result.getData().getEmail());
    assertEquals(anotherUser.getUsername(), result.getData().getUsername());
    assertEquals("another-token", result.getData().getToken());
  }

  @Test
  void getMe_withTokenWithoutBearer_extractsToken() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token mytoken", dataFetchingEnvironment);

    assertNotNull(result);
    assertEquals("mytoken", result.getData().getToken());
  }

  @Test
  void getMe_setsLocalContext() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertNotNull(result.getLocalContext());
    assertEquals(user, result.getLocalContext());
  }
}
