package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
  @Mock private DataFetchingEnvironment dfe;

  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("a@b.com", "alice", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User current) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(meDatafetcher.getMe("Token jwt-token", dfe)).isNull();
  }

  @Test
  public void should_return_null_when_principal_is_null() {
    login(null);

    assertThat(meDatafetcher.getMe("Token jwt-token", dfe)).isNull();
  }

  @Test
  public void should_throw_not_found_when_user_data_missing() {
    login(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meDatafetcher.getMe("Token jwt-token", dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_return_current_user_with_token_from_header() {
    login(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), "a@b.com", "alice", "bio", "image")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", dfe);

    assertThat(result.getData().getEmail()).isEqualTo("a@b.com");
    assertThat(result.getData().getUsername()).isEqualTo("alice");
    assertThat(result.getData().getToken()).isEqualTo("jwt-token");
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  public void should_build_user_payload_user_from_local_context() {
    when(dfe.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertThat(result.getData().getEmail()).isEqualTo("a@b.com");
    assertThat(result.getData().getUsername()).isEqualTo("alice");
    assertThat(result.getData().getToken()).isEqualTo("generated-token");
    assertThat(result.getLocalContext()).isSameAs(user);
  }
}
