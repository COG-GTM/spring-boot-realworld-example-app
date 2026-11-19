package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;
  private UserMutation userMutation;
  private User user;

  static class Registration {
    @NotBlank(message = "can't be empty")
    String email;

    Registration(String email) {
      this.email = email;
    }
  }

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);
    user = new User("alice@example.com", "alice", "encoded", "", "");
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

  // ---- createUser ----

  @Test
  public void create_user_should_register_and_return_user_payload_with_user_context() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("alice@example.com")
            .username("alice")
            .password("secret")
            .build();
    when(userService.createUser(any())).thenReturn(user);
    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    verify(userService).createUser(captor.capture());
    assertEquals("alice@example.com", captor.getValue().getEmail());
    assertEquals("alice", captor.getValue().getUsername());
    assertEquals("secret", captor.getValue().getPassword());
    assertTrue(result.getData() instanceof UserPayload);
    assertSame(user, result.getLocalContext());
  }

  @Test
  public void create_user_should_return_error_data_on_constraint_violation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    ConstraintViolationException cve =
        new ConstraintViolationException(validator.validate(new Registration("")));
    when(userService.createUser(any())).thenThrow(cve);
    CreateUserInput input =
        CreateUserInput.newBuilder().email("").username("alice").password("secret").build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertTrue(result.getData() instanceof Error);
    Error error = (Error) result.getData();
    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
    assertEquals(Collections.singletonList("can't be empty"), error.getErrors().get(0).getValue());
    assertNull(result.getLocalContext());
  }

  // ---- login ----

  @Test
  public void login_should_return_user_payload_when_password_matches() {
    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("secret", "alice@example.com");

    assertTrue(result.getData() instanceof UserPayload);
    assertSame(user, result.getLocalContext());
    verify(passwordEncoder).matches("secret", "encoded");
  }

  @Test
  public void login_should_reject_wrong_password() {
    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "alice@example.com"));
  }

  @Test
  public void login_should_reject_unknown_email() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("secret", "nobody@example.com"));
    verifyNoInteractions(passwordEncoder);
  }

  // ---- updateUser ----

  @Test
  public void update_user_should_update_current_user() {
    authenticate(user);
    UpdateUserInput changes =
        UpdateUserInput.newBuilder()
            .username("alice2")
            .email("alice2@example.com")
            .bio("new bio")
            .password("newpw")
            .image("new.png")
            .build();
    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);

    DataFetcherResult<UserPayload> result = userMutation.updateUser(changes);

    verify(userService).updateUser(captor.capture());
    UpdateUserCommand command = captor.getValue();
    assertSame(user, command.getTargetUser());
    assertEquals("alice2", command.getParam().getUsername());
    assertEquals("alice2@example.com", command.getParam().getEmail());
    assertEquals("new bio", command.getParam().getBio());
    assertEquals("newpw", command.getParam().getPassword());
    assertEquals("new.png", command.getParam().getImage());
    assertTrue(result.getData() instanceof UserPayload);
    assertSame(user, result.getLocalContext());
  }

  @Test
  public void update_user_should_return_null_for_anonymous() {
    anonymous();

    assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().bio("x").build()));
    verifyNoInteractions(userService);
  }

  @Test
  public void update_user_should_return_null_when_principal_is_null() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(null, null));

    assertNull(userMutation.updateUser(UpdateUserInput.newBuilder().bio("x").build()));
    verifyNoInteractions(userService);
  }
}
