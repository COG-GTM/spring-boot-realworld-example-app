package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @Captor private ArgumentCaptor<RegisterParam> registerParamCaptor;
  @Captor private ArgumentCaptor<UpdateUserCommand> updateUserCommandCaptor;

  @InjectMocks private UserMutation userMutation;

  private final User currentUser = new User("jake@jake.jake", "jake", "encoded", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(registerParamCaptor.capture())).thenReturn(currentUser);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder()
                .email("jake@jake.jake")
                .username("jake")
                .password("123")
                .build());

    assertEquals("jake@jake.jake", registerParamCaptor.getValue().getEmail());
    assertEquals("jake", registerParamCaptor.getValue().getUsername());
    assertEquals("123", registerParamCaptor.getValue().getPassword());
    assertTrue(result.getData() instanceof UserPayload);
    assertSame(currentUser, result.getLocalContext());
  }

  @Test
  public void should_return_error_result_when_registration_is_invalid() {
    when(userService.createUser(any()))
        .thenThrow(ConstraintViolationFixture.propertyPathViolations());

    DataFetcherResult<UserResult> result =
        userMutation.createUser(CreateUserInput.newBuilder().build());

    Error error = (Error) result.getData();
    assertEquals("BAD_REQUEST", error.getMessage());
    List<String> fields =
        error.getErrors().stream().map(ErrorItem::getKey).collect(Collectors.toList());
    assertTrue(fields.contains("email"));
    assertTrue(fields.contains("username"));
    assertNull(result.getLocalContext());
  }

  @Test
  public void should_login_with_matching_password() {
    when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(currentUser));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "jake@jake.jake");

    assertSame(currentUser, result.getLocalContext());
  }

  @Test
  public void should_reject_login_with_wrong_password() {
    when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.of(currentUser));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrong", "jake@jake.jake"));
  }

  @Test
  public void should_reject_login_of_unknown_email() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("123", "nobody@example.com"));
  }

  @Test
  public void should_update_current_user() {
    SecurityContextHelper.authenticate(currentUser);

    DataFetcherResult<UserPayload> result =
        userMutation.updateUser(
            UpdateUserInput.newBuilder()
                .email("new@example.com")
                .username("new-jake")
                .password("new-password")
                .bio("new bio")
                .image("new image")
                .build());

    verify(userService).updateUser(updateUserCommandCaptor.capture());
    UpdateUserParam param = updateUserCommandCaptor.getValue().getParam();
    assertSame(currentUser, updateUserCommandCaptor.getValue().getTargetUser());
    assertEquals("new@example.com", param.getEmail());
    assertEquals("new-jake", param.getUsername());
    assertEquals("new-password", param.getPassword());
    assertEquals("new bio", param.getBio());
    assertEquals("new image", param.getImage());
    assertSame(currentUser, result.getLocalContext());
  }

  @Test
  public void should_not_update_user_for_anonymous_request() {
    SecurityContextHelper.anonymous();

    assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().build()));
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_not_update_user_when_principal_is_null() {
    SecurityContextHelper.authenticateWithNullPrincipal();

    assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().build()));
  }
}
