package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  public void should_return_unauthenticated_error_for_invalid_authentication_exception() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(
            parametersFor(new io.spring.api.exception.InvalidAuthenticationException()));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(Arrays.asList("currentUser"), error.getPath());
    assertEquals(ErrorType.UNAUTHENTICATED.name(), error.getExtensions().get("errorType"));
  }

  @Test
  public void should_return_bad_request_error_with_field_extensions_for_constraint_violation() {
    Set<ConstraintViolation<RegisterParam>> violations =
        validator.validate(new RegisterParam("ab", "not-an-email", new Profile("")));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(exception.getMessage(), error.getMessage());
    assertEquals(Arrays.asList("currentUser"), error.getPath());

    Map<String, Object> extensions = error.getExtensions();
    assertEquals(ErrorType.BAD_REQUEST.name(), extensions.get("errorType"));
    assertEquals(Arrays.asList("size must be between 3 and 20"), extensions.get("username"));
    assertEquals(Arrays.asList("must be a well-formed email address"), extensions.get("email"));
    assertEquals(Arrays.asList("must not be blank"), extensions.get(""));
  }

  @Test
  public void should_group_multiple_messages_of_the_same_field_into_one_extension_entry() {
    Set<ConstraintViolation<RegisterParam>> violations =
        validator.validate(new RegisterParam("", "valid@email.com", new Profile("bio")));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    Object usernameMessages = result.getErrors().get(0).getExtensions().get("username");
    assertTrue(usernameMessages instanceof List);
    List<?> messages = (List<?>) usernameMessages;
    assertEquals(2, messages.size());
    assertTrue(messages.contains("must not be blank"));
    assertTrue(messages.contains("size must be between 3 and 20"));
  }

  @Test
  public void should_strip_method_and_argument_prefix_from_nested_property_path()
      throws NoSuchMethodException {
    Method method = UserService.class.getMethod("register", RegisterParam.class);
    UserService service = new UserService();
    Set<ConstraintViolation<UserService>> violations =
        validator
            .forExecutables()
            .validateParameters(
                service,
                method,
                new Object[] {new RegisterParam("", "valid@email.com", new Profile("bio"))});
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertTrue(
        extensions.containsKey("username"),
        "expected the method and argument segments to be stripped, got " + extensions.keySet());
    assertFalse(extensions.containsKey("register.arg0.username"));
  }

  @Test
  public void should_delegate_to_default_handler_for_unknown_exception() {
    RuntimeException exception = new RuntimeException("something went wrong");

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(ErrorType.INTERNAL.name(), error.getExtensions().get("errorType"));
    assertEquals("java.lang.RuntimeException: something went wrong", error.getMessage());
    assertEquals(Arrays.asList("currentUser"), error.getPath());
  }

  @Test
  public void should_delegate_graphql_authentication_exception_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new AuthenticationException()));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(ErrorType.INTERNAL.name(), error.getExtensions().get("errorType"));
    assertEquals("io.spring.graphql.exception.AuthenticationException: null", error.getMessage());
  }

  @Test
  public void should_build_error_data_from_constraint_violation_exception() {
    Set<ConstraintViolation<RegisterParam>> violations =
        validator.validate(new RegisterParam("", "not-an-email", new Profile("bio")));

    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(violations));

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(2, error.getErrors().size());
    assertEquals(Arrays.asList("must be a well-formed email address"), messagesOf(error, "email"));
    List<String> usernameMessages = messagesOf(error, "username");
    assertEquals(2, usernameMessages.size());
    assertTrue(usernameMessages.contains("must not be blank"));
    assertTrue(usernameMessages.contains("size must be between 3 and 20"));
  }

  @Test
  public void should_build_empty_error_data_when_there_is_no_violation() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(Collections.emptySet()));

    assertEquals("BAD_REQUEST", error.getMessage());
    assertTrue(error.getErrors().isEmpty());
  }

  private List<String> messagesOf(Error error, String key) {
    return error.getErrors().stream()
        .filter(item -> item.getKey().equals(key))
        .map(ErrorItem::getValue)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no error item for key " + key));
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable exception) {
    ExecutionStepInfo executionStepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.parse("/currentUser"))
            .build();
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    when(environment.getExecutionStepInfo()).thenReturn(executionStepInfo);
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  static class RegisterParam {
    @NotBlank
    @Size(min = 3, max = 20)
    private final String username;

    @Email private final String email;

    @Valid private final Profile profile;

    RegisterParam(String username, String email, Profile profile) {
      this.username = username;
      this.email = email;
      this.profile = profile;
    }

    public String getUsername() {
      return username;
    }

    public String getEmail() {
      return email;
    }

    public Profile getProfile() {
      return profile;
    }
  }

  static class Profile {
    @NotBlank private final String bio;

    Profile(String bio) {
      this.bio = bio;
    }

    public String getBio() {
      return bio;
    }
  }

  static class UserService {
    public void register(@Valid RegisterParam param) {}
  }
}
