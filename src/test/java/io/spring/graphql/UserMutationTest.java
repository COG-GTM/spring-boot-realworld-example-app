package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "encoded-password", "", "default-avatar");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void logIn(User loginUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(loginUser, null));
  }

  // ── createUser ──

  @Test
  void createUser_success() {
    when(userService.createUser(any())).thenReturn(user);

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("john@jacob.com")
            .username("johnjacob")
            .password("123")
            .build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void createUser_validationError() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<Object> violation = createMockViolation(
        "createUser.registerParam.email", "can't be empty");
    violations.add(violation);

    ConstraintViolationException cve = new ConstraintViolationException(violations);
    when(userService.createUser(any())).thenThrow(cve);

    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("")
            .username("johnjacob")
            .password("123")
            .build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNull(result.getLocalContext());
  }

  // ── login ──

  @Test
  void login_success() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "john@jacob.com");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_wrongPassword() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "john@jacob.com"));
  }

  @Test
  void login_emailNotFound() {
    when(userRepository.findByEmail(eq("nobody@test.com"))).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("123", "nobody@test.com"));
  }

  // ── updateUser ──

  @Test
  void updateUser_success() {
    logIn(user);

    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .username("newname")
            .email("new@email.com")
            .bio("new bio")
            .image("new-image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any());
  }

  @Test
  void updateUser_anonymousReturnsNull() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                java.util.Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    UpdateUserInput input =
        UpdateUserInput.newBuilder().username("newname").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
  }

  @Test
  void updateUser_nullPrincipalReturnsNull() {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    UpdateUserInput input =
        UpdateUserInput.newBuilder().username("newname").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<Object> createMockViolation(String propertyPath, String message) {
    Path path = org.mockito.Mockito.mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);

    java.lang.annotation.Annotation annotation =
        new javax.validation.constraints.NotBlank() {
          @Override
          public String message() {
            return message;
          }

          @Override
          public Class<?>[] groups() {
            return new Class<?>[0];
          }

          @Override
          public Class<? extends javax.validation.Payload>[] payload() {
            return new Class[0];
          }

          @Override
          public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return javax.validation.constraints.NotBlank.class;
          }
        };

    ConstraintDescriptor descriptor = org.mockito.Mockito.mock(ConstraintDescriptor.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);

    ConstraintViolation violation = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn((Class) Object.class);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    return violation;
  }
}
