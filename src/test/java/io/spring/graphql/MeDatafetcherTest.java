package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;
  private User currentUser;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    currentUser = new User("email@test.com", "username", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
  }

  @Test
  public void should_return_current_user_with_token() {
    authenticate();
    when(userQueryService.findById(currentUser.getId()))
        .thenReturn(
            Optional.of(
                new UserData(currentUser.getId(), "email@test.com", "username", "bio", "image")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment);

    assertThat(result.getData().getEmail(), is("email@test.com"));
    assertThat(result.getData().getUsername(), is("username"));
    assertThat(result.getData().getToken(), is("jwt-token"));
    assertThat(result.getLocalContext(), is(currentUser));
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment), nullValue());
  }

  @Test
  public void should_throw_when_current_user_is_missing() {
    authenticate();
    when(userQueryService.findById(currentUser.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_build_user_payload_from_local_context() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(currentUser);
    when(jwtService.toToken(currentUser)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result.getData().getUsername(), is("username"));
    assertThat(result.getData().getToken(), is("generated-token"));
  }
}
