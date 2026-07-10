package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class UserMutationTest extends DgsGraphQLTestBase {

  @Test
  void should_create_user() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\","
                + " password: \"123\"}) { ... on UserPayload { user { email username token } } } }",
            "data.createUser.user.token");

    assertEquals("jwt-token", token);
  }

  @Test
  void should_return_error_when_create_user_invalid() {
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    String message =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createUser(input: {email: \"bad\", username: \"u\", password: \"p\"}) { ..."
                + " on Error { message } } }",
            "data.createUser.message");

    assertEquals("BAD_REQUEST", message);
  }

  @Test
  void should_login() {
    setAnonymous();
    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(password: \"123\", email: \"john@jacob.com\") { user { email token }"
                + " } }",
            "data.login.user.token");

    assertEquals("jwt-token", token);
  }

  @Test
  void should_reject_login_when_invalid() {
    setAnonymous();
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { login(password: \"wrong\", email: \"nobody@test.com\") { user {"
                        + " email } } }",
                    "data.login"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_update_user() {
    setAuthenticatedUser(user);
    when(jwtService.toToken(eq(user))).thenReturn("jwt-token");

    String email =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {email: \"new@jacob.com\", bio: \"new bio\"}) { user {"
                + " email } } }",
            "data.updateUser.user.email");

    assertEquals(user.getEmail(), email);
    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  void should_return_null_update_user_when_anonymous() {
    setAnonymous();

    User result =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateUser(changes: {email: \"new@jacob.com\"}) { user { email } } }",
            "data.updateUser");

    assertNull(result);
  }
}
