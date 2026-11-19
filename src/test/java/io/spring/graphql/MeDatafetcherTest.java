package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {
  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;
  private DataFetchingEnvironment dfe;
  private User user;

  @BeforeEach
  public void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    dfe = mock(DataFetchingEnvironment.class);
    user = new User("a@b.com", "alice", "pw", "bio", "img.png");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void getMe_should_return_current_user_with_token_from_header() {
    authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(Optional.of(new UserData(user.getId(), "a@b.com", "alice", "bio", "img.png")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token abc.def.ghi", dfe);

    assertThat(result.getData().getEmail()).isEqualTo("a@b.com");
    assertThat(result.getData().getUsername()).isEqualTo("alice");
    assertThat(result.getData().getToken()).isEqualTo("abc.def.ghi");
    assertThat((Object) result.getLocalContext()).isSameAs(user);
    verify(userQueryService).findById(user.getId());
  }

  @Test
  public void getMe_should_return_null_for_anonymous_user() {
    anonymous();

    assertThat(meDatafetcher.getMe(null, dfe)).isNull();
    verify(userQueryService, never()).findById(any());
  }

  @Test
  public void getMe_should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList()));

    assertThat(meDatafetcher.getMe("Token x", dfe)).isNull();
  }

  @Test
  public void getMe_should_throw_not_found_when_user_data_missing() {
    authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meDatafetcher.getMe("Token abc", dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void getUserPayloadUser_should_build_user_with_fresh_token() {
    when(dfe.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("new-token");

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertThat(result.getData().getEmail()).isEqualTo("a@b.com");
    assertThat(result.getData().getUsername()).isEqualTo("alice");
    assertThat(result.getData().getToken()).isEqualTo("new-token");
    assertThat((Object) result.getLocalContext()).isSameAs(user);
    verify(jwtService).toToken(user);
  }
}
