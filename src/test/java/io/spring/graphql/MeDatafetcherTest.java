package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
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

  @InjectMocks private MeDatafetcher meDatafetcher;

  @Mock private DataFetchingEnvironment dfe;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("e@t.com", "testuser", "pass", "bio", "img");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getMe_should_return_user_when_authenticated() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
    UserData userData =
        new UserData(user.getId(), user.getEmail(), user.getUsername(), "bio", "img");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token mytoken", dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("e@t.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
  }

  @Test
  void getMe_should_return_null_when_anonymous() {
    AnonymousAuthenticationToken anonAuth =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token mytoken", dfe);

    assertNull(result);
  }

  @Test
  void getUserPayloadUser_should_return_user_with_token() {
    when(dfe.getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("jwt-token");

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertNotNull(result);
    assertEquals("e@t.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("jwt-token", result.getData().getToken());
  }
}
