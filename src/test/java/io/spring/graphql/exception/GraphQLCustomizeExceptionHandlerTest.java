package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.graphql.types.Error;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  void should_get_errors_as_data_from_constraint_violation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    TestBean bean = new TestBean();
    Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
    assertFalse(violations.isEmpty());

    Set<ConstraintViolation<?>> wildcardViolations = new HashSet<>(violations);
    ConstraintViolationException cve = new ConstraintViolationException(wildcardViolations);
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_create_handler_instance() {
    assertNotNull(handler);
  }

  static class TestBean {
    @NotBlank(message = "can't be empty")
    private String name;
  }
}
