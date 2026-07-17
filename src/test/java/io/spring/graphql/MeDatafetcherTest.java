package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;

  private final User user = new User("user@example.com", "username", "123", "bio", "image");

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User principal) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, null));
  }

  @Test
  void should_return_current_user_with_token_from_authorization_header() {
    authenticate(user);
    UserData userData = new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token-value", dataFetchingEnvironment);

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("jwt-token-value");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_return_null_when_authentication_is_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(meDatafetcher.getMe("Token jwt-token-value", dataFetchingEnvironment)).isNull();
  }

  @Test
  void should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(meDatafetcher.getMe("Token jwt-token-value", dataFetchingEnvironment)).isNull();
  }

  @Test
  void should_throw_not_found_when_user_missing() {
    authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> meDatafetcher.getMe("Token jwt-token-value", dataFetchingEnvironment));
  }

  @Test
  void should_build_user_payload_user_from_local_context() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result.getData().getEmail()).isEqualTo(user.getEmail());
    assertThat(result.getData().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getData().getToken()).isEqualTo("generated-token");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }
}
