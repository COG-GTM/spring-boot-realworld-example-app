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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;

  @BeforeEach
  void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getMe_should_return_user_when_authenticated() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(user, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserData userData = new UserData(user.getId(), "test@example.com", "testuser", "bio", "image");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token test-token", dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test@example.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("test-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void getMe_should_return_null_when_anonymous() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token test-token", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_should_return_null_when_principal_is_null() {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token test-token", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  void getMe_should_throw_when_user_not_found() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(user, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token test-token", dataFetchingEnvironment));
  }

  @Test
  void getUserPayloadUser_should_return_user_from_local_context() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image");
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test@example.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }
}
