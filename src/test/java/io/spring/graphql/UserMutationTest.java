package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
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
import io.spring.graphql.types.ErrorItem;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserService userService;

  private UserMutation userMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    userMutation = new UserMutation(userRepository, passwordEncoder, userService);
    user = new User("a@test.com", "a", "encoded", "", "");
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);
    CreateUserInput input =
        CreateUserInput.newBuilder().email("a@test.com").username("a").password("123").build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    ArgumentCaptor<RegisterParam> captor = ArgumentCaptor.forClass(RegisterParam.class);
    verify(userService).createUser(captor.capture());
    Assertions.assertEquals("a@test.com", captor.getValue().getEmail());
    Assertions.assertEquals("a", captor.getValue().getUsername());
    Assertions.assertEquals("123", captor.getValue().getPassword());
    Assertions.assertTrue(result.getData() instanceof UserPayload);
    Assertions.assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_return_errors_as_data_when_registration_is_invalid() {
    when(userService.createUser(any())).thenThrow(ConstraintViolationFixture.blankRegistration());
    CreateUserInput input =
        CreateUserInput.newBuilder().email("").username("").password("").build();

    DataFetcherResult<UserResult> result = userMutation.createUser(input);

    Assertions.assertTrue(result.getData() instanceof Error);
    Error error = (Error) result.getData();
    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Assertions.assertEquals(
        Set.of("email", "username", "password"),
        error.getErrors().stream().map(ErrorItem::getKey).collect(Collectors.toSet()));
    Assertions.assertNull(result.getLocalContext());
  }

  @Test
  public void should_login_with_correct_password() {
    when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("123", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("123", "a@test.com");

    Assertions.assertNotNull(result.getData());
    Assertions.assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_fail_login_with_wrong_password() {
    when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

    Assertions.assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("wrong", "a@test.com"));
  }

  @Test
  public void should_fail_login_with_unknown_email() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        InvalidAuthenticationException.class, () -> userMutation.login("123", "unknown@test.com"));
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  public void should_update_current_user() {
    GraphQLTestSupport.authenticate(user);
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("b@test.com")
            .username("b")
            .bio("bio")
            .password("456")
            .image("image")
            .build();

    DataFetcherResult<UserPayload> result = userMutation.updateUser(input);

    ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
    verify(userService).updateUser(captor.capture());
    UpdateUserCommand command = captor.getValue();
    Assertions.assertSame(user, command.getTargetUser());
    Assertions.assertEquals("b@test.com", command.getParam().getEmail());
    Assertions.assertEquals("b", command.getParam().getUsername());
    Assertions.assertEquals("bio", command.getParam().getBio());
    Assertions.assertEquals("456", command.getParam().getPassword());
    Assertions.assertEquals("image", command.getParam().getImage());
    Assertions.assertSame(user, result.getLocalContext());
  }

  @Test
  public void should_return_null_when_updating_as_anonymous() {
    GraphQLTestSupport.anonymous();

    Assertions.assertNull(
        userMutation.updateUser(UpdateUserInput.newBuilder().username("b").build()));
    verifyNoInteractions(userService);
  }
}
