package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
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
import io.spring.application.user.RegisterParam;
import io.spring.graphql.types.Error;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

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

  private DataFetcherExceptionHandlerParameters parameters(Throwable exception) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    when(environment.getExecutionStepInfo())
        .thenReturn(
            ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLString)
                .path(ResultPath.parse("/createUser"))
                .build());
    when(environment.getMergedField())
        .thenReturn(MergedField.newMergedField(Field.newField("createUser").build()).build());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .exception(exception)
        .dataFetchingEnvironment(environment)
        .build();
  }

  @Test
  public void should_return_unauthenticated_error_for_invalid_authentication() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getExtensions().get("errorType"))
        .isEqualTo(ErrorType.UNAUTHENTICATED.toString());
    assertThat(error.getPath()).containsExactly("createUser");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_return_bad_request_error_with_field_extensions_for_constraint_violations() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation("createUser.arg0.email", "can't be empty"));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.onException(parameters(exception));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions().get("errorType")).isEqualTo(ErrorType.BAD_REQUEST.toString());
    Map<String, Object> extensions = error.getExtensions();
    assertThat((List<String>) extensions.get("email")).containsExactly("can't be empty");
  }

  @Test
  public void should_delegate_to_default_handler_for_other_exceptions() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(new RuntimeException("boom")));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
    assertThat(result.getErrors().get(0).getExtensions()).doesNotContainKey("email");
  }

  @Test
  public void should_group_multiple_violations_on_same_field_as_data() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation("createUser.arg0.email", "can't be empty"));
    violations.add(violation("createUser.arg0.email", "should be an email"));
    violations.add(violation("username", "can't be empty"));

    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(violations));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(2);
    assertThat(error.getErrors().stream().map(e -> e.getKey()))
        .containsExactlyInAnyOrder("email", "username");
    List<String> emailMessages =
        error.getErrors().stream()
            .filter(e -> e.getKey().equals("email"))
            .findFirst()
            .get()
            .getValue();
    assertThat(emailMessages)
        .containsExactlyInAnyOrderElementsOf(Arrays.asList("can't be empty", "should be an email"));
    List<String> usernameMessages =
        error.getErrors().stream()
            .filter(e -> e.getKey().equals("username"))
            .findFirst()
            .get()
            .getValue();
    assertThat(usernameMessages).isEqualTo(Collections.singletonList("can't be empty"));
  }

  @Test
  public void should_treat_authentication_exception_with_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(new AuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getPath()).containsExactly("createUser");
  }
}
