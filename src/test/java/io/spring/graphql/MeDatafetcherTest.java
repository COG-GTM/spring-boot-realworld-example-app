package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.graphql.types.UserPayload;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;

  @Mock private JwtService jwtService;

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @Mock private SecurityContext securityContext;

  @Mock private Authentication authentication;

  @InjectMocks private MeDatafetcher meDatafetcher;

  private User user;
  private UserData userData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "test bio", "test-image.jpg");
    userData =
        new UserData(user.getId(), "test@example.com", "testuser", "test bio", "test-image.jpg");
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  public void should_get_user_payload_user() {
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(any())).thenReturn("test-token");

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertThat(result).isNotNull();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEmail()).isEqualTo("test@example.com");
    assertThat(result.getData().getUsername()).isEqualTo("testuser");
    assertThat(result.getData().getToken()).isEqualTo("test-token");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  public void should_return_null_when_not_authenticated() {
    when(securityContext.getAuthentication()).thenReturn(new AnonymousAuthenticationToken("key", "principal", java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token", dataFetchingEnvironment);

    assertThat(result).isNull();
  }

  @Test
  public void should_get_me_successfully() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer test-token", dataFetchingEnvironment);

    assertThat(result).isNotNull();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getEmail()).isEqualTo("test@example.com");
    assertThat(result.getData().getUsername()).isEqualTo("testuser");
    assertThat(result.getData().getToken()).isEqualTo("test-token");
    assertThat(result.getLocalContext()).isEqualTo(user);
  }
}
