package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private static class Payload {
    @NotBlank(message = "can't be empty")
    private final String title;

    Payload(String title) {
      this.title = title;
    }
  }

  private ConstraintViolationException violationOf(Payload payload) {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Payload>> violations = validator.validate(payload);
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_convert_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(violationOf(new Payload("")));

    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors().size(), is(1));
    ErrorItem item = error.getErrors().get(0);
    assertThat(item.getKey(), is("title"));
    assertThat(item.getValue(), is(java.util.Arrays.asList("can't be empty")));
  }

  @Test
  public void should_return_empty_errors_when_nothing_is_violated() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(violationOf(new Payload("a title")));

    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors().isEmpty(), is(true));
  }
}
