package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.application.user.UpdateUserCommand;
import io.spring.core.user.User;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserMutationTest extends GraphQLTestBase {

  static class SampleBean {
    @NotBlank(message = "can't be empty")
    private final String email;

    SampleBean(String email) {
      this.email = email;
    }
  }

  @Test
  public void should_create_user() {
    when(userService.createUser(any())).thenReturn(user);

    String createdUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\", password: \"123\"}) { ... on UserPayload { user { username email token } } } }",
            "data.createUser.user.username");

    Assertions.assertEquals(username, createdUsername);
  }

  @Test
  public void should_return_error_payload_on_invalid_create_user() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<SampleBean>> violations = validator.validate(new SampleBean(""));
    when(userService.createUser(any())).thenThrow(new ConstraintViolationException(violations));

    String message =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \"\", username: \"\", password: \"\"}) { ... on Error { message errors { key value } } } }",
            "data.createUser.message");

    Assertions.assertEquals("BAD_REQUEST", message);
  }

  @Test
  public void should_login_with_valid_credentials() {
    when(userRepository.findByEmail(eq(email))).thenReturn(java.util.Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    String loggedInUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(email: \"john@jacob.com\", password: \"123\") { user { username token } } }",
            "data.login.user.username");

    Assertions.assertEquals(username, loggedInUsername);
  }

  @Test
  public void should_return_unauthenticated_error_on_invalid_login() {
    when(userRepository.findByEmail(eq(email))).thenReturn(java.util.Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"john@jacob.com\", password: \"wrong\") { user { username } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_update_user_when_authenticated() {
    String updatedUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {email: \"new@email.com\", bio: \"new bio\"}) { user { username } } }",
            "data.updateUser.user.username");

    Assertions.assertEquals(username, updatedUsername);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_return_null_update_user_when_anonymous() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateUser(changes: {bio: \"x\"}) { user { username } } }");

    Assertions.assertTrue(result.getErrors().isEmpty());
    java.util.Map<String, Object> data = result.getData();
    Assertions.assertNull(data.get("updateUser"));
  }
}
