package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;

  private final User user = new User("me@example.com", "me", "123", "bio", "image");

  @BeforeEach
  void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_me_when_authenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    when(userQueryService.findById(user.getId()))
        .thenReturn(
            Optional.of(new UserData(user.getId(), "me@example.com", "me", "bio", "image")));

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class));

    assertEquals("me", result.getData().getUsername());
    assertEquals("jwt-token", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void should_return_null_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertNull(meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class)));
  }

  @Test
  void should_throw_when_user_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token jwt-token", mock(DataFetchingEnvironment.class)));
  }

  @Test
  void should_build_user_payload_user_from_local_context() {
    when(jwtService.toToken(user)).thenReturn("generated-token");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);

    DataFetcherResult<io.spring.graphql.types.User> result = meDatafetcher.getUserPayloadUser(dfe);

    assertEquals("me", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
  }
}
