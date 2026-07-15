package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserMutationTest {

  private UserRepository userRepository;
  private PasswordEncoder encryptService;
  private UserService userService;
  private UserMutation userMutation;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    encryptService = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @Test
  void createUser_success_returnsUserPayloadWithUserContext() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("user@example.com")
            .username("user")
            .password("123")
            .build();
    User created = new User("user@example.com", "user", "123", "", "image");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(created);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertTrue(result.getData() instanceof UserPayload);
    assertEquals(created, result.getLocalContext());
  }

  @Test
  void createUser_constraintViolation_returnsErrorData() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("user@example.com")
            .username("user")
            .password("123")
            .build();
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(singleViolation()));

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertTrue(result.getData() instanceof io.spring.graphql.types.Error);
    assertFalse(result.getData() instanceof UserPayload);
  }

  private Set<ConstraintViolation<?>> singleViolation() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<Bean>> violations = validator.validate(new Bean());
      return new HashSet<>(violations);
    }
  }

  private static class Bean {
    @NotBlank(message = "can't be empty")
    private String name;
  }

  @Test
  void login_success_returnsUserPayloadWithUserContext() {
    User user = new User("user@example.com", "user", "encoded", "", "image");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "user@example.com");

    assertTrue(result.getData() instanceof UserPayload);
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_emailNotFound_throwsInvalidAuthentication() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("123", "user@example.com"));
  }

  @Test
  void login_passwordMismatch_throwsInvalidAuthentication() {
    User user = new User("user@example.com", "user", "encoded", "", "image");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "user@example.com"));
  }

  @Test
  void updateUser_authenticated_updatesAndReturnsPayload() {
    User user = new User("user@example.com", "user", "123", "bio", "image");
    authenticate(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@example.com")
            .username("newname")
            .bio("newbio")
            .password("newpass")
            .image("newimage")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    verify(userService).updateUser(any(UpdateUserCommand.class));
    assertTrue(result.getData() instanceof UserPayload);
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void updateUser_anonymous_returnsNull() {
    anonymous();
    UpdateUserInput input = UpdateUserInput.newBuilder().email("new@example.com").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
  }
}
