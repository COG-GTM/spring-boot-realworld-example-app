package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
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

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest extends GraphqlTestBase {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  @InjectMocks private MeDatafetcher meDatafetcher;

  private User user;
  private UserData userData;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "image");
  }

  @Test
  void should_get_current_user_with_the_token_of_the_request() {
    login(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", environment);

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("jwt-token");
    assertThat((User) result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_get_null_when_anonymous() {
    logout();

    assertThat(meDatafetcher.getMe("Token jwt-token", environment)).isNull();
  }

  @Test
  void should_fail_when_current_user_is_gone() {
    login(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> meDatafetcher.getMe("Token jwt-token", environment));
  }

  @Test
  void should_build_user_payload_with_a_fresh_token() {
    when(environment.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("new-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(environment);

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("new-token");
    assertThat((User) result.getLocalContext()).isEqualTo(user);
  }
}
