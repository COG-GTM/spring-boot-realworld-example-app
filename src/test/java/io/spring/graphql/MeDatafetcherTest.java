package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.graphql.types.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest extends GraphQLTestBase {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  @InjectMocks private MeDatafetcher meDatafetcher;

  private UserData userData() {
    return new UserData(
        user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
  }

  @Test
  void should_get_me_with_token_from_authorization_header() {
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData()));

    DataFetcherResult<User> result = meDatafetcher.getMe("Token jwt-token", dfe(null, null));

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("jwt-token");
    assertThat((Object) result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_return_null_when_anonymous() {
    anonymous();

    assertThat(meDatafetcher.getMe("Token jwt-token", dfe(null, null))).isNull();
  }

  @Test
  void should_fail_when_user_not_found() {
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> meDatafetcher.getMe("Token jwt-token", dfe(null, null)));
  }

  @Test
  void should_get_user_payload_user_from_local_context() {
    when(jwtService.toToken(eq(user))).thenReturn("new-token");

    DataFetcherResult<User> result = meDatafetcher.getUserPayloadUser(dfe(null, user));

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getToken()).isEqualTo("new-token");
  }
}
