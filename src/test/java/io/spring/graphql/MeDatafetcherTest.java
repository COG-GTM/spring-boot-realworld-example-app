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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;

  @Mock private JwtService jwtService;

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @InjectMocks private MeDatafetcher meDatafetcher;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getMe_authenticated() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    UserData userData =
        new UserData(user.getId(), "test@example.com", "testuser", "bio", "image.png");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    String authorization = "Token mytoken123";

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe(authorization, dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test@example.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("mytoken123", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void getMe_anonymous() {
    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_nullPrincipal() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_userNotFound() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token mytoken", dataFetchingEnvironment));
  }

  @Test
  void getUserPayloadUser_success() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generatedToken");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test@example.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("generatedToken", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }
}
