package io.spring.graphql;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import io.spring.graphql.types.ErrorItem;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
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
  private PasswordEncoder encryptService;
  private UserService userService;
  private UserMutation userMutation;

  private final User user = new User("john@example.com", "john", "encoded", "bio", "image");

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    encryptService = mock(PasswordEncoder.class);
    userService = mock(UserService.class);
    userMutation = new UserMutation(userRepository, encryptService, userService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_user_success() {
    when(userService.createUser(any())).thenReturn(user);

    DataFetcherResult<UserResult> result =
        userMutation.createUser(new CreateUserInput("john@example.com", "john", "123"));

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    assertThat(captor.getValue().getEmail(), is("john@example.com"));
    assertThat(captor.getValue().getUsername(), is("john"));
    assertThat(captor.getValue().getPassword(), is("123"));

    assertThat(result.getData(), instanceOf(UserPayload.class));
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_return_errors_as_data_when_create_user_violates_constraints() {
    when(userService.createUser(any())).thenThrow(invalidEmailException());

    DataFetcherResult<UserResult> result = userMutation.createUser(new CreateUserInput("", "", ""));

    assertThat(result.getData(), instanceOf(io.spring.graphql.types.Error.class));
    io.spring.graphql.types.Error error = (io.spring.graphql.types.Error) result.getData();
    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(
        error.getErrors().stream().map(ErrorItem::getKey).collect(Collectors.toList()),
        hasItem("email"));
    assertThat(
        error.getErrors().stream()
            .filter(item -> item.getKey().equals("email"))
            .findFirst()
            .get()
            .getValue(),
        hasItem("should be an email"));
    assertThat(result.getLocalContext(), is(nullValue()));
  }

  @Test
  public void should_login_success() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "john@example.com");

    assertThat(result.getData(), notNullValue());
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "john@example.com"));
  }

  @Test
  public void should_fail_login_with_unknown_email() {
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("123", "unknown@example.com"));
    verify(encryptService, never()).matches(any(), any());
  }

  @Test
  public void should_update_user_success() {
    setCurrentUser(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("new@example.com")
            .username("newname")
            .password("newpassword")
            .bio("new bio")
            .image("new image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getTargetUser(), is(user));
    assertThat(captor.getValue().getParam().getEmail(), is("new@example.com"));
    assertThat(captor.getValue().getParam().getUsername(), is("newname"));
    assertThat(captor.getValue().getParam().getPassword(), is("newpassword"));
    assertThat(captor.getValue().getParam().getBio(), is("new bio"));
    assertThat(captor.getValue().getParam().getImage(), is("new image"));

    assertThat(result.getData(), notNullValue());
    assertThat(result.getLocalContext(), is(user));
  }

  @Test
  public void should_update_user_with_partial_changes() {
    setCurrentUser(user);

    userMutation.updateUser(UpdateUserInput.newBuilder().bio("only bio").build());

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    assertThat(captor.getValue().getParam().getBio(), is("only bio"));
    assertThat(captor.getValue().getParam().getEmail(), is(nullValue()));
    assertThat(captor.getValue().getParam().getUsername(), is(nullValue()));
  }

  @Test
  public void should_get_null_when_updating_user_anonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(
        userMutation.updateUser(UpdateUserInput.newBuilder().bio("bio").build()), is(nullValue()));
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_throw_null_pointer_when_updating_user_without_authentication() {
    SecurityContextHolder.clearContext();

    assertThrows(
        NullPointerException.class,
        () -> userMutation.updateUser(UpdateUserInput.newBuilder().bio("bio").build()));
    verify(userService, never()).updateUser(any());
  }

  @Test
  public void should_propagate_validation_error_when_updating_user() {
    setCurrentUser(user);
    doThrow(invalidEmailException()).when(userService).updateUser(any());

    assertThrows(
        ConstraintViolationException.class,
        () -> userMutation.updateUser(UpdateUserInput.newBuilder().email("taken").build()));
  }

  private void setCurrentUser(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                currentUser, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
  }

  private ConstraintViolationException invalidEmailException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<UpdateUserParam>> violations =
        validator.validate(UpdateUserParam.builder().email("not-an-email").build());
    assertThat(violations.isEmpty(), is(false));
    return new ConstraintViolationException(violations);
  }
}
