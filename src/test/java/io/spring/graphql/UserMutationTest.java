package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;
  @InjectMocks private UserMutation mutation;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_successfully() {
    CreateUserInput input =
        CreateUserInput.newBuilder().email("a@b.com").username("alice").password("secret").build();
    User user = new User("a@b.com", "alice", "secret", "", "");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = mutation.createUser(input);

    assertNotNull(result);
    assertSame(user, result.getLocalContext());
    assertTrue(result.getData() instanceof UserPayload);
  }

  @Test
  public void should_return_error_payload_when_constraint_violation() {
    CreateUserInput input =
        CreateUserInput.newBuilder().email("bad").username("a").password("x").build();
    HashSet<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(buildViolation());
    ConstraintViolationException ex = new ConstraintViolationException(violations);
    when(userService.createUser(any(RegisterParam.class))).thenThrow(ex);

    DataFetcherResult<UserResult> result = mutation.createUser(input);

    assertNotNull(result);
    assertTrue(result.getData() instanceof Error);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> buildViolation() {
    Path path = org.mockito.Mockito.mock(Path.class);
    org.mockito.Mockito.doReturn("createUser.input.email").when(path).toString();
    ConstraintDescriptor<?> descriptor = org.mockito.Mockito.mock(ConstraintDescriptor.class);
    java.lang.annotation.Annotation annotation =
        new java.lang.annotation.Annotation() {
          @Override
          public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return Override.class;
          }
        };
    org.mockito.Mockito.doReturn(annotation).when(descriptor).getAnnotation();
    ConstraintViolation<?> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
    org.mockito.Mockito.doReturn(path).when(violation).getPropertyPath();
    org.mockito.Mockito.doReturn("invalid").when(violation).getMessage();
    org.mockito.Mockito.doReturn(Object.class).when(violation).getRootBeanClass();
    org.mockito.Mockito.doReturn(descriptor).when(violation).getConstraintDescriptor();
    return violation;
  }

  @Test
  public void should_login_successfully() {
    User user = new User("a@b.com", "alice", "encoded", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("secret", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = mutation.login("secret", "a@b.com");

    assertNotNull(result);
    assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_throw_invalid_authentication_when_user_not_found() {
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

    assertThrows(InvalidAuthenticationException.class, () -> mutation.login("secret", "a@b.com"));
  }

  @Test
  public void should_throw_invalid_authentication_when_password_mismatch() {
    User user = new User("a@b.com", "alice", "encoded", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(InvalidAuthenticationException.class, () -> mutation.login("wrong", "a@b.com"));
  }

  @Test
  public void should_update_user_when_authenticated() {
    User user = new User("a@b.com", "alice", "secret", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@b.com")
            .username("alice2")
            .bio("bio")
            .password("p")
            .image("img")
            .build();

    DataFetcherResult<UserPayload> result = mutation.updateUser(input);

    assertNotNull(result);
    assertSame(user, result.getLocalContext());
    verify(userService, times(1)).updateUser(any());
  }
}
