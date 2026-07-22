package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MeDatafetcherTest extends GraphQLTestBase {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @Test
  public void should_return_me_when_authenticated() {
    User user = new User("email@test.com", "username", "pass", "bio", "image");
    setCurrentUser(user);
    UserData userData = new UserData(user.getId(), "email@test.com", "username", "bio", "image");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment);

    assertThat(result.getData().getEmail(), is("email@test.com"));
    assertThat(result.getData().getUsername(), is("username"));
    assertThat(result.getData().getToken(), is("jwt-token"));
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_return_null_when_anonymous() {
    setAnonymous();
    assertThat(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment), is(nullValue()));
  }

  @Test
  public void should_throw_when_user_data_missing() {
    User user = new User("email@test.com", "username", "pass", "bio", "image");
    setCurrentUser(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_build_user_payload_user_with_generated_token() {
    User user = new User("email@test.com", "username", "pass", "bio", "image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(eq(user))).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result.getData().getEmail(), is("email@test.com"));
    assertThat(result.getData().getUsername(), is("username"));
    assertThat(result.getData().getToken(), is("generated-token"));
    assertThat(result.getLocalContext(), is(user));
  }
}
