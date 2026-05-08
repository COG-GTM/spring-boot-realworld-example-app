package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
  @Mock private DataFetchingEnvironment dfe;
  @InjectMocks private MeDatafetcher datafetcher;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_null_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "k", "anon", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));

    assertNull(datafetcher.getMe("Token abc", dfe));
  }

  @Test
  public void should_return_user_with_token() {
    User currentUser = new User("a@b.com", "alice", "secret", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
    UserData userData = new UserData(currentUser.getId(), "a@b.com", "alice", "", "");
    when(userQueryService.findById(currentUser.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result = datafetcher.getMe("Token abc", dfe);

    assertNotNull(result);
    assertEquals("a@b.com", result.getData().getEmail());
    assertEquals("alice", result.getData().getUsername());
    assertEquals("abc", result.getData().getToken());
    assertSame(currentUser, result.getLocalContext());
  }

  @Test
  public void should_return_user_payload_user() {
    User currentUser = new User("a@b.com", "alice", "secret", "", "");
    when(dfe.getLocalContext()).thenReturn(currentUser);
    when(jwtService.toToken(currentUser)).thenReturn("token-xyz");

    DataFetcherResult<io.spring.graphql.types.User> result = datafetcher.getUserPayloadUser(dfe);

    assertNotNull(result.getData());
    assertEquals("a@b.com", result.getData().getEmail());
    assertEquals("alice", result.getData().getUsername());
    assertEquals("token-xyz", result.getData().getToken());
  }
}
