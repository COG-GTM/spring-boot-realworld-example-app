package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.graphql.types.UserPayload;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    user = new User("test@test.com", "testuser", "password", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_null_for_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anon",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    var result = meDatafetcher.getMe("Bearer token", null);
    assertNull(result);
  }

  @Test
  void should_return_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    UserData userData = new UserData(user.getId(), "test@test.com", "testuser", "", "");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    var result = meDatafetcher.getMe("Bearer mytoken", null);

    assertNotNull(result);
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("mytoken", result.getData().getToken());
  }

  @Test
  void should_resolve_user_payload() {
    when(jwtService.toToken(any())).thenReturn("generated-token");

    graphql.schema.DataFetchingEnvironment dfe = mock(graphql.schema.DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);

    var result = meDatafetcher.getUserPayloadUser(dfe);

    assertNotNull(result);
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
  }
}
