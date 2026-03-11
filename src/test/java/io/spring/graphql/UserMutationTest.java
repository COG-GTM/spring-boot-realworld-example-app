package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;
  @Mock private SecurityContext securityContext;

  private UserMutation userMutation;
  private User user;

  @BeforeEach
  void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
  }

  @Test
  void createUser_withValidInput_returnsUserPayload() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("new@test.com")
            .username("newuser")
            .password("password123")
            .build();

    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertTrue(result.getData() instanceof UserPayload);
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any(RegisterParam.class));
  }

  @Test
  void createUser_withConstraintViolation_returnsError() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("invalid")
            .username("")
            .password("short")
            .build();

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);

    java.lang.annotation.Annotation annotation = mock(java.lang.annotation.Annotation.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("email");
    doReturn(RegisterParam.class).when(violation).getRootBeanClass();
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(javax.validation.constraints.Email.class).when(annotation).annotationType();
    when(violation.getMessage()).thenReturn("must be a valid email");
    violations.add(violation);

    ConstraintViolationException cve = new ConstraintViolationException(violations);
    when(userService.createUser(any(RegisterParam.class))).thenThrow(cve);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void login_withValidCredentials_returnsUserPayload() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("password", user.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("password", "test@test.com");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void login_withInvalidEmail_throwsInvalidAuthenticationException() {
    when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("password", "nonexistent@test.com"));
  }

  @Test
  void login_withInvalidPassword_throwsInvalidAuthenticationException() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrongpassword", user.getPassword())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrongpassword", "test@test.com"));
  }

  @Test
  void updateUser_withAuthenticatedUser_returnsUserPayload() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .username("updateduser")
            .email("updated@test.com")
            .bio("updated bio")
            .image("updated image")
            .password("newpassword")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  void updateUser_withAnonymousAuthentication_returnsNull() {
    AnonymousAuthenticationToken anonymousToken =
        new AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    when(securityContext.getAuthentication()).thenReturn(anonymousToken);
    SecurityContextHolder.setContext(securityContext);

    UpdateUserInput input = UpdateUserInput.newBuilder().username("updateduser").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }

  @Test
  void updateUser_withNullPrincipal_returnsNull() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    UpdateUserInput input = UpdateUserInput.newBuilder().username("updateduser").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNull(result);
    verify(userService, never()).updateUser(any());
  }

  @Test
  void updateUser_withPartialUpdate_updatesOnlyProvidedFields() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    UpdateUserInput input = UpdateUserInput.newBuilder().bio("new bio only").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  void createUser_passesCorrectParametersToService() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("new@test.com")
            .username("newuser")
            .password("password123")
            .build();

    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    userMutation.createUser(input);

    verify(userService)
        .createUser(
            argThat(
                param ->
                    param.getEmail().equals("new@test.com")
                        && param.getUsername().equals("newuser")
                        && param.getPassword().equals("password123")));
  }

  @Test
  void login_withEmptyPassword_throwsInvalidAuthenticationException() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("", user.getPassword())).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("", "test@test.com"));
  }

  @Test
  void updateUser_withAllFieldsNull_stillCallsService() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    UpdateUserInput input = UpdateUserInput.newBuilder().build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertNotNull(result);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }
}
