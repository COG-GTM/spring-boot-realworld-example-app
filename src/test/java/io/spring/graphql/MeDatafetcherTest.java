package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class MeDatafetcherTest {

  private UserQueryService userQueryService;
  private JwtService jwtService;
  private MeDatafetcher meDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    userQueryService = mock(UserQueryService.class);
    jwtService = mock(JwtService.class);
    meDatafetcher = new MeDatafetcher(userQueryService, jwtService);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_null_when_anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token jwt", dfe);
    assertNull(result);
  }

  @Test
  public void should_return_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));

    UserData userData =
        new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getImage());
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Token myjwt", dfe);
    assertNotNull(result);
    assertEquals("test@test.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("myjwt", result.getData().getToken());
  }

  @Test
  public void should_get_user_payload_user() {
    when(jwtService.toToken(eq(user))).thenReturn("generated-token");

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(user);

    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dfe);
    assertNotNull(result);
    assertEquals("test@test.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
  }
}
