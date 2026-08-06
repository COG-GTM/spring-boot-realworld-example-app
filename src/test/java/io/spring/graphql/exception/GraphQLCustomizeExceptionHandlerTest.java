package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.TagsQueryService;
import io.spring.application.user.UpdateUserParam;
import io.spring.graphql.TagDatafetcher;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      TagDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
public class GraphQLCustomizeExceptionHandlerTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  static class UserUpdater {
    public void update(@Valid UpdateUserParam param) {}
  }

  private static ConstraintViolationException invalidEmailViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<UpdateUserParam>> violations =
        validator.validate(UpdateUserParam.builder().email("not-an-email").build());
    return new ConstraintViolationException(violations);
  }

  private static ConstraintViolationException invalidMethodParameterViolation() throws Exception {
    ExecutableValidator validator =
        Validation.buildDefaultValidatorFactory().getValidator().forExecutables();
    UserUpdater updater = new UserUpdater();
    Set<ConstraintViolation<UserUpdater>> violations =
        validator.validateParameters(
            updater,
            UserUpdater.class.getMethod("update", UpdateUserParam.class),
            new Object[] {UpdateUserParam.builder().email("not-an-email").build()});
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    when(tagsQueryService.allTags()).thenThrow(new InvalidAuthenticationException());

    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getExtensions().get("errorType")).isEqualTo("UNAUTHENTICATED");
    assertThat(error.getPath()).containsExactly("tags");
  }

  @Test
  public void should_expose_constraint_violations_as_error_extensions() {
    when(tagsQueryService.allTags()).thenThrow(invalidEmailViolation());

    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");

    assertThat(result.getErrors()).hasSize(1);
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertThat((List<String>) extensions.get("email")).containsExactly("should be an email");
  }

  @Test
  public void should_fall_back_to_the_default_handler_for_other_exceptions() {
    when(tagsQueryService.allTags()).thenThrow(new ResourceNotFoundException());

    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions().get("errorType"))
        .isNotEqualTo("UNAUTHENTICATED");
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(invalidEmailViolation());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    ErrorItem item = error.getErrors().get(0);
    assertThat(item.getKey()).isEqualTo("email");
    assertThat(item.getValue()).containsExactly("should be an email");
  }

  @Test
  public void should_strip_method_and_parameter_from_violation_property_path() throws Exception {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(invalidMethodParameterViolation());

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
  }
}
