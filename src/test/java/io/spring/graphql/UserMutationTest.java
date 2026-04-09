package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;

  @BeforeEach
  void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createUser_should_return_user_payload_on_success() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("test@example.com")
            .username("testuser")
            .password("password123")
            .build();
    User user = new User("test@example.com", "testuser", "encoded", "", "");
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void createUser_should_return_error_on_constraint_violation() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("bad-email")
            .username("")
            .password("password123")
            .build();

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("registerParam.email");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("should be an email");
    when(violation.getRootBeanClass()).thenReturn((Class) Object.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    Override annotation = mock(Override.class);
    doReturn(Override.class).when(annotation).annotationType();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException cve = new ConstraintViolationException(violations);
    when(userService.createUser(any())).thenThrow(cve);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertInstanceOf(io.spring.graphql.types.Error.class, result.getData());
    io.spring.graphql.types.Error error = (io.spring.graphql.types.Error) result.getData();
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNull(result.getLocalContext());
    verify(userService).createUser(any());
  }

  @Test
  void login_should_return_user_payload_on_success() {
    String email = "test@example.com";
    String password = "password123";
    User user = new User(email, "testuser", "encodedPassword", "", "");

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(encryptService.matches(password, user.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login(password, email);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_should_throw_when_user_not_found() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("password", "unknown@example.com"));
  }

  @Test
  void login_should_throw_when_password_does_not_match() {
    String email = "test@example.com";
    User user = new User(email, "testuser", "encodedPassword", "", "");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(encryptService.matches("wrongpassword", user.getPassword())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrongpassword", email));
  }

  @Test
  void updateUser_should_return_user_payload_when_authenticated() {
    User currentUser = new User("test@example.com", "testuser", "encoded", "bio", "image");
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(currentUser, null, "ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UpdateUserInput updateInput =
        UpdateUserInput.newBuilder()
            .email("new@example.com")
            .username("newname")
            .bio("new bio")
            .password("newpass")
            .image("newimage")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertInstanceOf(UserPayload.class, result.getData());
    assertEquals(currentUser, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  void updateUser_should_return_null_when_anonymous() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);

    UpdateUserInput updateInput = UpdateUserInput.newBuilder().email("new@example.com").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }

  @Test
  void updateUser_should_return_null_when_principal_is_null() {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UpdateUserInput updateInput = UpdateUserInput.newBuilder().email("new@example.com").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }
}
