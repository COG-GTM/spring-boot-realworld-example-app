package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.reflect.Method;
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
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new InvalidAuthenticationException(), "/login"));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getPath()).containsExactly("login");
    assertThat(error.getExtensions()).containsEntry("errorType", "UNAUTHENTICATED");
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_error_with_field_extensions() {
    ConstraintViolationException cve = registerViolations();

    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(cve, "/createUser"));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getPath()).containsExactly("createUser");
    assertThat(error.getExtensions()).containsEntry("errorType", "BAD_REQUEST");
    assertThat(error.getExtensions().get("email")).isEqualTo(List.of("should be an email"));
    assertThat(error.getExtensions().get("username")).isEqualTo(List.of("can't be empty"));
  }

  @Test
  public void should_delegate_unknown_exceptions_to_the_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new RuntimeException("boom"), "/article"));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("java.lang.RuntimeException: boom");
    assertThat(error.getExtensions()).containsEntry("errorType", "INTERNAL");
  }

  @Test
  public void should_convert_constraint_violations_into_error_payload() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(registerViolations());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(2);
    assertThat(error.getErrors().stream().map(ErrorItem::getKey))
        .containsExactlyInAnyOrder("email", "username");
    ErrorItem emailItem =
        error.getErrors().stream()
            .filter(item -> item.getKey().equals("email"))
            .findFirst()
            .orElseThrow();
    assertThat(emailItem.getValue()).containsExactly("should be an email");
  }

  @Test
  public void should_group_multiple_messages_of_the_same_field_together() {
    Set<ConstraintViolation<RegistrationForm>> violations =
        validator.validate(new RegistrationForm("not-an-email", "someone"));
    // a whitespace-only email violates both @NotBlank and @Email
    Set<ConstraintViolation<RegistrationForm>> blankEmail =
        validator.validate(new RegistrationForm("   ", "someone"));
    assertThat(violations).hasSize(1);
    assertThat(blankEmail).hasSize(2);

    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(blankEmail));

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(error.getErrors().get(0).getValue())
        .containsExactlyInAnyOrder("can't be empty", "should be an email");
  }

  @Test
  public void should_strip_method_and_parameter_prefix_from_nested_property_paths()
      throws Exception {
    Method method = RegistrationService.class.getMethod("createUser", RegistrationForm.class);
    Set<ConstraintViolation<RegistrationService>> violations =
        validator
            .forExecutables()
            .validateParameters(
                new RegistrationService(),
                method,
                new Object[] {new RegistrationForm("not-an-email", "someone", new Address(""))});
    assertThat(violations.stream().map(v -> v.getPropertyPath().toString()))
        .containsExactlyInAnyOrder("createUser.form.email", "createUser.form.address.city");

    DataFetcherExceptionHandlerResult result =
        handler.onException(
            parametersFor(new ConstraintViolationException(violations), "/createUser"));

    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertThat(extensions.get("email")).isEqualTo(List.of("should be an email"));
    assertThat(extensions.get("address.city")).isEqualTo(List.of("can't be empty"));
  }

  private ConstraintViolationException registerViolations() {
    Set<ConstraintViolation<RegistrationForm>> violations =
        validator.validate(new RegistrationForm("not-an-email", ""));
    assertThat(violations).hasSize(2);
    return new ConstraintViolationException(violations);
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable exception, String path) {
    DataFetchingEnvironment dataFetchingEnvironment =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.parse(path))
                    .build())
            .mergedField(
                MergedField.newMergedField(Field.newField(path.substring(1)).build()).build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .exception(exception)
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .build();
  }

  static class RegistrationForm {
    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    @Valid private final Address address;

    RegistrationForm(String email, String username) {
      this(email, username, null);
    }

    RegistrationForm(String email, String username, Address address) {
      this.email = email;
      this.username = username;
      this.address = address;
    }
  }

  static class Address {
    @NotBlank(message = "can't be empty")
    private final String city;

    Address(String city) {
      this.city = city;
    }
  }

  static class RegistrationService {
    public void createUser(@Valid RegistrationForm form) {}
  }
}
