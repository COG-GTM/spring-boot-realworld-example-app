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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("a@test.com", "aisensiy", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
  }

  @Test
  public void should_get_me_with_token_from_authorization_header() {
    authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(
            Optional.of(
                new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "image")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment);

    assertThat(result.getData().getEmail()).isEqualTo("a@test.com");
    assertThat(result.getData().getUsername()).isEqualTo("aisensiy");
    assertThat(result.getData().getToken()).isEqualTo("jwt-token");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment)).isNull();
  }

  @Test
  public void should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment)).isNull();
  }

  @Test
  public void should_throw_when_user_not_found() {
    authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_get_user_payload_user_with_generated_token() {
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("new-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result.getData().getEmail()).isEqualTo("a@test.com");
    assertThat(result.getData().getUsername()).isEqualTo("aisensiy");
    assertThat(result.getData().getToken()).isEqualTo("new-token");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }
}
