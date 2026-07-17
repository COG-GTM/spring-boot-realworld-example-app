package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
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

  private User user;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    encryptService = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("john@example.com", "john", "encoded", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  private void authenticateAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private ConstraintViolationException constraintViolation(String field, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(field);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn((Class) User.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) NotBlank.class);
    when(descriptor.getAnnotation()).thenReturn((Annotation) annotation);
    when(violation.getConstraintDescriptor()).thenReturn((ConstraintDescriptor) descriptor);
    Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
    return new ConstraintViolationException(violations);
  }

  @Test
  void should_create_user_and_expose_user_via_local_context() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("john@example.com")
            .username("john")
            .password("secret")
            .build();
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat(result.getLocalContext()).isEqualTo(user);
  }

  @Test
  void should_return_error_result_when_create_user_violates_constraints() {
    CreateUserInput input =
        CreateUserInput.newBuilder().email("bad").username("").password("").build();
    ConstraintViolationException cve = constraintViolation("email", "should be an email");
    when(userService.createUser(any(RegisterParam.class))).thenThrow(cve);

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(error.getErrors().get(0).getValue()).contains("should be an email");
    assertThat(result.getLocalContext()).isNull();
  }

  @Test
  void should_login_with_matching_password() {
    when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("secret"), eq(user.getPassword()))).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("secret", "john@example.com");

    assertThat(result.getLocalContext()).isEqualTo(user);
    assertThat(result.getData()).isNotNull();
  }

  @Test
  void should_reject_login_with_wrong_password() {
    when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrong"), eq(user.getPassword()))).thenReturn(false);

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("wrong", "john@example.com"));
  }

  @Test
  void should_reject_login_when_user_not_found() {
    when(userRepository.findByEmail(eq("ghost@example.com"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(InvalidAuthenticationException.class)
        .isThrownBy(() -> userMutation.login("secret", "ghost@example.com"));
  }

  @Test
  void should_update_current_user() {
    authenticateAs(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@example.com")
            .username("newname")
            .bio("new bio")
            .password("newpass")
            .image("new.png")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertThat(result.getLocalContext()).isEqualTo(user);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  void should_return_null_when_updating_user_anonymously() {
    authenticateAnonymous();
    UpdateUserInput input = UpdateUserInput.newBuilder().email("x@x.com").build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    assertThat(result).isNull();
    verify(userService, never()).updateUser(any());
  }
}
