package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.graphql.types.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @InjectMocks private MeDatafetcher meDatafetcher;

  private final io.spring.core.user.User user =
      new io.spring.core.user.User("jake@jake.jake", "jake", "123", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_return_current_user_with_token_from_authorization_header() {
    SecurityContextHelper.authenticate(user);
    when(userQueryService.findById(user.getId()))
        .thenReturn(
            Optional.of(new UserData(user.getId(), "jake@jake.jake", "jake", "bio", "image")));

    DataFetcherResult<User> result =
        meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment);

    assertEquals("jake@jake.jake", result.getData().getEmail());
    assertEquals("jake", result.getData().getUsername());
    assertEquals("jwt-token", result.getData().getToken());
    assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_return_null_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertNull(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_return_null_when_principal_is_null() {
    SecurityContextHelper.authenticateWithNullPrincipal();

    assertNull(meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_throw_when_current_user_is_not_found() {
    SecurityContextHelper.authenticate(user);
    when(userQueryService.findById(user.getId())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> meDatafetcher.getMe("Token jwt-token", dataFetchingEnvironment));
  }

  @Test
  public void should_build_user_payload_user_with_generated_token() {
    when(dataFetchingEnvironment.<io.spring.core.user.User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("generated-token");

    DataFetcherResult<User> result = meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertEquals("jake@jake.jake", result.getData().getEmail());
    assertEquals("jake", result.getData().getUsername());
    assertEquals("generated-token", result.getData().getToken());
    assertSame(user, result.getLocalContext());
  }
}
