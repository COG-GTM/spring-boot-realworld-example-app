package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
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

class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher datafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    datafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  @Test
  void getMe_returns_user_with_token_when_authenticated() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "", "");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        datafetcher.getMe("Token jwt-token", dfe);

    assertEquals(user.getEmail(), result.getData().getEmail());
    assertEquals(user.getUsername(), result.getData().getUsername());
    assertEquals("jwt-token", result.getData().getToken());
  }

  @Test
  void getMe_returns_null_when_anonymous() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);

    assertNull(datafetcher.getMe("Token jwt-token", dfe));
  }

  @Test
  void getMe_throws_when_user_not_found() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.getMe("Token jwt-token", dfe));
  }

  @Test
  void getUserPayloadUser_builds_user_from_local_context() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    org.mockito.Mockito.doReturn(user).when(dfe).getLocalContext();
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result = datafetcher.getUserPayloadUser(dfe);

    assertEquals(user.getEmail(), result.getData().getEmail());
    assertEquals("generated-token", result.getData().getToken());
  }
}
