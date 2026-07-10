package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeDatafetcherTest extends GraphQLTestBase {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;

  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = newUser();
  }

  @Test
  void should_get_me_when_authenticated() {
    setCurrentUser(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(new UserData(user.getId(), "john@jacob.com", "johnjacob", "", "avatar")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-abc", mockEnv());

    assertThat(result.getData().getUsername()).isEqualTo("johnjacob");
    assertThat(result.getData().getEmail()).isEqualTo("john@jacob.com");
    assertThat(result.getData().getToken()).isEqualTo("jwt-abc");
  }

  @Test
  void should_return_null_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(meDatafetcher.getMe("Token jwt-abc", mockEnv())).isNull();
  }

  @Test
  void should_return_null_when_principal_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertThat(meDatafetcher.getMe("Token jwt-abc", mockEnv())).isNull();
  }

  @Test
  void should_throw_when_user_not_found() {
    setCurrentUser(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meDatafetcher.getMe("Token jwt-abc", mockEnv()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_user_payload_user_from_local_context() {
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(any(User.class))).thenReturn("generated-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dfe);

    assertThat(result.getData().getUsername()).isEqualTo("johnjacob");
    assertThat(result.getData().getToken()).isEqualTo("generated-token");
  }
}
