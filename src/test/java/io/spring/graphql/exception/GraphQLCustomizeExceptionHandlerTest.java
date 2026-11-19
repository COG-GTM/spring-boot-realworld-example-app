package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
  private DataFetchingEnvironment env;
  private Validator validator;

  public static class Param {
    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private String email;

    @NotBlank(message = "can't be empty")
    private String username;

    Param(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }

  public static class Service {
    public void createUser(@Valid Param param) {}
  }

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    env = mock(DataFetchingEnvironment.class);
    when(env.getExecutionStepInfo())
        .thenReturn(
            ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLString)
                .path(ResultPath.parse("/createUser"))
                .build());
    when(env.getMergedField())
        .thenReturn(MergedField.newMergedField(Field.newField("createUser").build()).build());
    when(env.getField()).thenReturn(Field.newField("createUser").build());
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerResult handle(Throwable e) {
    return handler.onException(
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(e)
            .build());
  }

  private ConstraintViolationException methodViolation(Param param) throws Exception {
    Method method = Service.class.getMethod("createUser", Param.class);
    Set<ConstraintViolation<Service>> violations =
        validator.forExecutables().validateParameters(new Service(), method, new Object[] {param});
    return new ConstraintViolationException("createUser: invalid", violations);
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result = handle(new InvalidAuthenticationException());

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getPath()).containsExactly("createUser");
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handle(methodViolation(new Param("not-an-email", "")));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("createUser: invalid");
    assertThat(error.getPath()).containsExactly("createUser");
    Map<String, Object> extensions = error.getExtensions();
    assertThat(extensions).containsEntry("errorType", ErrorType.BAD_REQUEST.name());
    assertThat(extensions).containsEntry("email", Arrays.asList("should be an email"));
    assertThat(extensions).containsEntry("username", Arrays.asList("can't be empty"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result = handle(new IllegalStateException("boom"));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error).isInstanceOf(TypedGraphQLError.class);
    assertThat(error.getMessage()).isEqualTo("java.lang.IllegalStateException: boom");
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.INTERNAL.name());
    assertThat(error.getPath()).containsExactly("createUser");
  }

  @Test
  public void getErrorsAsData_should_group_messages_by_field() throws Exception {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(methodViolation(new Param("", "")));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    Map<String, List<String>> byKey =
        error.getErrors().stream()
            .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
    assertThat(byKey).containsOnlyKeys("email", "username");
    assertThat(byKey.get("email")).containsExactly("can't be empty");
    assertThat(byKey.get("username")).containsExactly("can't be empty");
  }

  @Test
  public void getErrorsAsData_should_merge_multiple_messages_for_same_field() throws Exception {
    Param param = new Param(" ", "bob");
    ConstraintViolationException cve = methodViolation(param);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(new HashSet<>(error.getErrors().get(0).getValue()))
        .containsExactlyInAnyOrder("can't be empty", "should be an email");
  }

  @Test
  public void getErrorsAsData_should_keep_single_segment_property_path() {
    Set<ConstraintViolation<Param>> violations = validator.validate(new Param("a@b.com", ""));

    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(violations));

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("username");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
  }

  @Test
  public void getErrorsAsData_should_return_empty_errors_when_no_violations() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(new HashSet<ConstraintViolation<?>>()));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).isEmpty();
  }
}
