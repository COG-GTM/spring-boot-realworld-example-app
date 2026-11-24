package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      UserMutation.class,
      MeDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
public class UserMutationTest {
  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserRepository userRepository;
  @MockBean private PasswordEncoder passwordEncoder;
  @MockBean private UserService userService;
  @MockBean private UserQueryService userQueryService;
  @MockBean private JwtService jwtService;
  @MockBean private ProfileQueryService profileQueryService;

  private User user;

  private static final String CREATE_USER =
      "mutation { createUser(input: {email: \"john@jacob.com\", username: \"johnjacob\", "
          + "password: \"123\"}) { __typename "
          + "... on UserPayload { user { email username token } } "
          + "... on Error { message errors { key value } } } }";

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "encrypted", "", "");
    when(jwtService.toToken(any())).thenReturn("jwt-token");
  }

  @Test
  public void should_create_user_and_return_user_payload() {
    when(userService.createUser(any())).thenReturn(user);

    ExecutionResult result = dgsQueryExecutor.execute(CREATE_USER);

    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    Assertions.assertEquals(
        "UserPayload",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.__typename"));
    Assertions.assertEquals(
        "johnjacob",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.user.username"));
    Assertions.assertEquals(
        "jwt-token",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.user.token"));
  }

  @Test
  public void should_return_error_union_when_create_user_is_invalid() {
    Set<ConstraintViolation<InvalidParam>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(new InvalidParam());
    when(userService.createUser(any())).thenThrow(new ConstraintViolationException(violations));

    Assertions.assertEquals(
        "Error",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.__typename"));
    Assertions.assertEquals(
        "BAD_REQUEST",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.message"));
    Assertions.assertEquals(
        "username",
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.errors[0].key"));
    List<String> messages =
        dgsQueryExecutor.executeAndExtractJsonPath(CREATE_USER, "data.createUser.errors[0].value");
    Assertions.assertEquals(Collections.singletonList("can't be empty"), messages);
  }

  @Test
  public void should_login_with_correct_password() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(eq("123"), eq("encrypted"))).thenReturn(true);

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { login(email: \"john@jacob.com\", password: \"123\") { user { token } } }",
            "data.login.user.token");

    Assertions.assertEquals("jwt-token", token);
  }

  @Test
  public void should_return_unauthenticated_error_with_wrong_password() {
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"john@jacob.com\", password: \"wrong\") { user { token } } }");

    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertEquals("invalid email or password", error.getMessage());
    Assertions.assertEquals(Collections.singletonList("login"), error.getPath());
    Assertions.assertEquals(
        "UNAUTHENTICATED", String.valueOf(error.getExtensions().get("errorType")));
    Assertions.assertNull(result.<Map<String, Object>>getData().get("login"));
  }

  static class InvalidParam {
    @NotBlank(message = "can't be empty")
    private String username = "";
  }
}
