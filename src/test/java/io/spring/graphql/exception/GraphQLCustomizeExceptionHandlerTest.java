package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
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
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import javax.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private ValidatorFactory validatorFactory;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterEach
  public void tearDown() {
    validatorFactory.close();
  }

  @Test
  public void should_map_invalid_authentication_exception_to_unauthenticated_error() {
    ResultPath path = ResultPath.parse("/currentUser");
    DataFetcherExceptionHandlerParameters parameters =
        parametersFor(new InvalidAuthenticationException(), path);

    DataFetcherExceptionHandlerResult result = handler.onException(parameters);

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(path.toList(), error.getPath());
  }

  @Test
  public void should_map_constraint_violation_exception_to_bad_request_with_field_extensions() {
    ConstraintViolationException cve = constraintViolationOf(new TargetBean("", "valid content"));
    ResultPath path = ResultPath.parse("/createArticle");
    DataFetcherExceptionHandlerParameters parameters = parametersFor(cve, path);

    DataFetcherExceptionHandlerResult result = handler.onException(parameters);

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("BAD_REQUEST", error.getExtensions().get("errorType"));
    assertEquals(cve.getMessage(), error.getMessage());
    assertEquals(path.toList(), error.getPath());

    Map<String, Object> extensions = error.getExtensions();
    assertTrue(extensions.containsKey("title"), "expected the violated field to be reported");
    assertFalse(extensions.containsKey("content"), "valid field must not be reported");
    assertInstanceOf(List.class, extensions.get("title"));
    List<?> titleMessages = (List<?>) extensions.get("title");
    assertEquals(1, titleMessages.size());
    assertFalse(((String) titleMessages.get(0)).isEmpty());
  }

  @Test
  public void should_report_every_violated_field_in_extensions() {
    ConstraintViolationException cve = constraintViolationOf(new TargetBean("", ""));
    DataFetcherExceptionHandlerParameters parameters =
        parametersFor(cve, ResultPath.parse("/createArticle"));

    GraphQLError error = handler.onException(parameters).getErrors().get(0);
    Map<String, Object> extensions = error.getExtensions();

    assertEquals("BAD_REQUEST", extensions.get("errorType"));
    assertTrue(extensions.containsKey("title"));
    assertTrue(extensions.containsKey("content"));
  }

  @Test
  public void should_extract_field_name_from_method_parameter_property_path() {
    ConstraintViolationException cve = methodParameterViolationOf(new TargetBean("", "ok"));
    DataFetcherExceptionHandlerParameters parameters =
        parametersFor(cve, ResultPath.parse("/createArticle"));

    GraphQLError error = handler.onException(parameters).getErrors().get(0);
    Map<String, Object> extensions = error.getExtensions();

    assertTrue(
        extensions.containsKey("title"),
        "nested method-parameter path should be reduced to the leaf field name");
    assertFalse(extensions.containsKey("arg0.title"));
    assertFalse(extensions.containsKey("create.arg0.title"));
  }

  @Test
  public void should_delegate_unknown_exceptions_to_default_handler() {
    RuntimeException unexpected = new IllegalStateException("boom");
    DataFetcherExceptionHandlerParameters parameters =
        parametersFor(unexpected, ResultPath.parse("/articles"));

    DataFetcherExceptionHandlerResult result = handler.onException(parameters);

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("INTERNAL", error.getExtensions().get("errorType"));
    assertTrue(error.getMessage().contains("boom"));
    assertTrue(error.getMessage().contains(IllegalStateException.class.getName()));
  }

  @Test
  public void should_build_error_data_from_constraint_violations() {
    ConstraintViolationException cve = constraintViolationOf(new TargetBean("", ""));

    Error errorData = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", errorData.getMessage());
    List<ErrorItem> items = errorData.getErrors();
    assertEquals(2, items.size());
    for (ErrorItem item : items) {
      assertTrue(item.getKey().equals("title") || item.getKey().equals("content"));
      assertEquals(1, item.getValue().size());
      assertFalse(item.getValue().get(0).isEmpty());
    }
  }

  @Test
  public void should_group_multiple_messages_for_the_same_field() {
    ConstraintViolationException cve = constraintViolationOf(new MultiConstraintBean(""));

    Error errorData = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", errorData.getMessage());
    assertEquals(1, errorData.getErrors().size());
    ErrorItem item = errorData.getErrors().get(0);
    assertEquals("value", item.getKey());
    assertTrue(item.getValue().size() >= 2, "both violations on the field should be grouped");
  }

  @Test
  public void authentication_exception_is_a_runtime_exception() {
    AuthenticationException exception = new AuthenticationException();
    assertInstanceOf(RuntimeException.class, exception);
  }

  private DataFetcherExceptionHandlerParameters parametersFor(
      Throwable exception, ResultPath path) {
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(path);
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  private ConstraintViolationException constraintViolationOf(Object bean) {
    Set<ConstraintViolation<Object>> violations = validator.validate(bean);
    assertFalse(violations.isEmpty(), "test bean must produce constraint violations");
    return new ConstraintViolationException(violations);
  }

  private ConstraintViolationException methodParameterViolationOf(TargetBean bean) {
    try {
      ExecutableValidator executableValidator = validator.forExecutables();
      ArticleService service = new ArticleService();
      Method method = ArticleService.class.getMethod("create", TargetBean.class);
      Set<ConstraintViolation<ArticleService>> violations =
          executableValidator.validateParameters(service, method, new Object[] {bean});
      assertFalse(violations.isEmpty(), "method validation must produce violations");
      return new ConstraintViolationException(violations);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  static class TargetBean {
    @NotBlank private final String title;
    @NotBlank private final String content;

    TargetBean(String title, String content) {
      this.title = title;
      this.content = content;
    }
  }

  static class MultiConstraintBean {
    @NotBlank(message = "can't be empty")
    @javax.validation.constraints.Size(min = 3, message = "too short")
    private final String value;

    MultiConstraintBean(String value) {
      this.value = value;
    }
  }

  static class ArticleService {
    public void create(@Valid TargetBean bean) {}
  }
}
