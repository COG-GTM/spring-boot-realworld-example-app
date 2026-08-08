package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import java.util.Collections;
import java.util.Iterator;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    userMutation = new UserMutation(userRepository, encryptService, userService);
    user = new User("a@b.com", "alice", "encoded", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_and_return_payload() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder()
                .email("a@b.com")
                .username("alice")
                .password("secret")
                .build());

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("a@b.com");
    assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    assertThat(captor.getValue().getPassword()).isEqualTo("secret");
    assertThat(result.getData()).isInstanceOf(UserPayload.class);
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  public void should_return_error_data_when_create_user_violates_constraints() {
    Set<ConstraintViolation<?>> violations =
        Collections.singleton(violation("createUser.arg0.email", "can't be empty"));
    ConstraintViolationException exception = new ConstraintViolationException(violations);
    when(userService.createUser(any(RegisterParam.class))).thenThrow(exception);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(
            CreateUserInput.newBuilder().email("").username("alice").password("secret").build());

    assertThat(result.getData()).isInstanceOf(Error.class);
    Error error = (Error) result.getData();
    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
    assertThat(result.getLocalContext()).isNull();
  }

  private static class StubPath implements Path {
    private final String value;

    StubPath(String value) {
      this.value = value;
    }

    @Override
    public Iterator<Node> iterator() {
      return Collections.<Node>emptyList().iterator();
    }

    @Override
    public String toString() {
      return value;
    }
  }

  @SuppressWarnings("unchecked")
  private static ConstraintViolation<?> violation(String propertyPath, String message) {
    ConstraintViolation<RegisterParam> violation = mock(ConstraintViolation.class);
    Path path = new StubPath(propertyPath);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    NotBlank annotation = mock(NotBlank.class);
    doReturn(NotBlank.class).when(annotation).annotationType();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(RegisterParam.class).when(violation).getRootBeanClass();
    when(violation.getPropertyPath()).thenReturn(path);
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    when(violation.getMessage()).thenReturn(message);
    return violation;
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("secret", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("secret", "a@b.com");

    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isSameAs(user);
  }

  @Test
  public void should_throw_when_password_does_not_match() {
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThatThrownBy(() -> userMutation.login("wrong", "a@b.com"))
        .isInstanceOf(InvalidAuthenticationException.class);
  }

  @Test
  public void should_throw_when_user_not_found_on_login() {
    when(userRepository.findByEmail("missing@b.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userMutation.login("secret", "missing@b.com"))
        .isInstanceOf(InvalidAuthenticationException.class);
  }

  @Test
  public void should_return_null_when_updating_user_anonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_return_null_when_principal_is_null_on_update() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList()));

    assertThat(userMutation.updateUser(UpdateUserInput.newBuilder().build())).isNull();
  }

  @Test
  public void should_update_current_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    DataFetcherResult<UserPayload> result =
        userMutation.updateUser(
            UpdateUserInput.newBuilder()
                .email("new@b.com")
                .username("newname")
                .bio("new bio")
                .password("newpass")
                .image("new image")
                .build());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser()).isSameAs(user);
    assertThat(captor.getValue().getParam().getEmail()).isEqualTo("new@b.com");
    assertThat(captor.getValue().getParam().getUsername()).isEqualTo("newname");
    assertThat(captor.getValue().getParam().getBio()).isEqualTo("new bio");
    assertThat(captor.getValue().getParam().getImage()).isEqualTo("new image");
    assertThat(captor.getValue().getParam().getPassword()).isEqualTo("newpass");
    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isSameAs(user);
  }
}
