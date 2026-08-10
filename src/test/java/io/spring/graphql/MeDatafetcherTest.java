package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.graphql.types.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class MeDatafetcherTest {

  private final UserQueryService userQueryService = mock(UserQueryService.class);
  private final JwtService jwtService = mock(JwtService.class);
  private final MeDatafetcher datafetcher = new MeDatafetcher(userQueryService, jwtService);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private io.spring.core.user.User authenticate() {
    io.spring.core.user.User user =
        new io.spring.core.user.User("jake@jake.jake", "jake", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
    return user;
  }

  @Test
  void should_return_current_user_with_token_from_authorization_header() {
    io.spring.core.user.User user = authenticate();
    when(userQueryService.findById(user.getId()))
        .thenReturn(
            Optional.of(new UserData(user.getId(), "jake@jake.jake", "jake", "bio", "image")));

    DataFetcherResult<User> result =
        datafetcher.getMe("Token my-jwt-token", mock(DataFetchingEnvironment.class));

    assertThat(result.getData().getUsername()).isEqualTo("jake");
    assertThat(result.getData().getEmail()).isEqualTo("jake@jake.jake");
    assertThat(result.getData().getToken()).isEqualTo("my-jwt-token");
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  void should_return_null_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(datafetcher.getMe("Token my-jwt-token", mock(DataFetchingEnvironment.class)))
        .isNull();
  }

  @Test
  void should_return_null_when_principal_is_absent() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(datafetcher.getMe("Token my-jwt-token", mock(DataFetchingEnvironment.class)))
        .isNull();
  }

  @Test
  void should_throw_not_found_when_current_user_is_missing_in_read_model() {
    io.spring.core.user.User user = authenticate();
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThatThrownBy(() -> datafetcher.getMe("Token my-jwt-token", dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_build_user_payload_user_with_generated_token() {
    io.spring.core.user.User user =
        new io.spring.core.user.User("jake@jake.jake", "jake", "123", "bio", "image");
    when(jwtService.toToken(user)).thenReturn("generated-token");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.user.User>getLocalContext()).thenReturn(user);

    DataFetcherResult<User> result = datafetcher.getUserPayloadUser(dfe);

    assertThat(result.getData().getUsername()).isEqualTo("jake");
    assertThat(result.getData().getEmail()).isEqualTo("jake@jake.jake");
    assertThat(result.getData().getToken()).isEqualTo("generated-token");
    assertThat(result.getLocalContext()).isSameAs(user);
  }
}
