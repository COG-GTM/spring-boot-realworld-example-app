package io.spring.graphql;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {
  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("a@test.com", "a", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_return_current_user_with_token_from_header() {
    GraphQLTestSupport.authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), "a@test.com", "a", "bio", "image")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc", GraphQLTestSupport.env(null, null));

    Assertions.assertEquals("a@test.com", result.getData().getEmail());
    Assertions.assertEquals("a", result.getData().getUsername());
    Assertions.assertEquals("abc", result.getData().getToken());
    Assertions.assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    GraphQLTestSupport.anonymous();

    Assertions.assertNull(meDatafetcher.getMe("Token abc", GraphQLTestSupport.env(null, null)));
    verifyNoInteractions(userQueryService);
  }

  @Test
  public void should_fail_when_current_user_is_missing() {
    GraphQLTestSupport.authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token abc", GraphQLTestSupport.env(null, null)));
  }

  @Test
  public void should_build_user_payload_with_new_token() {
    when(jwtService.toToken(user)).thenReturn("jwt");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(GraphQLTestSupport.env(null, user));

    Assertions.assertEquals("a@test.com", result.getData().getEmail());
    Assertions.assertEquals("a", result.getData().getUsername());
    Assertions.assertEquals("jwt", result.getData().getToken());
    Assertions.assertSame(user, result.getLocalContext());
  }
}
