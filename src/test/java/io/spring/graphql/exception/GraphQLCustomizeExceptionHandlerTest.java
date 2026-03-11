package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.graphql.types.Error;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  public void should_get_errors_as_data_from_constraint_violation() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error, notNullValue());
    assertThat(error.getMessage(), equalTo("BAD_REQUEST"));
    assertThat(error.getErrors(), notNullValue());
    assertThat(error.getErrors().size(), equalTo(1));
    assertThat(error.getErrors().get(0).getKey(), equalTo("email"));
    assertThat(error.getErrors().get(0).getValue().get(0), equalTo("must be a valid email"));
  }

  @Test
  public void should_get_errors_as_data_with_multiple_violations_on_same_field() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    violations.add(createMockViolation("email", "cannot be empty"));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error, notNullValue());
    assertThat(error.getMessage(), equalTo("BAD_REQUEST"));
    assertThat(error.getErrors().size(), equalTo(1));
    assertThat(error.getErrors().get(0).getKey(), equalTo("email"));
    assertThat(error.getErrors().get(0).getValue().size(), equalTo(2));
  }

  @Test
  public void should_get_errors_as_data_with_multiple_fields() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    violations.add(createMockViolation("username", "cannot be empty"));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error, notNullValue());
    assertThat(error.getMessage(), equalTo("BAD_REQUEST"));
    assertThat(error.getErrors().size(), equalTo(2));
  }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ConstraintViolation<?> createMockViolation(String field, String message) {
      ConstraintViolation violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);

      when(path.toString()).thenReturn(field);
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn(message);
      when(violation.getRootBeanClass()).thenReturn(Object.class);
      when(violation.getConstraintDescriptor()).thenReturn(descriptor);
    
      // Create a real annotation instance for the mock to return
      javax.validation.constraints.NotNull notNullAnnotation = 
          new javax.validation.constraints.NotNull() {
            @Override
            public String message() {
              return message;
            }
            @Override
            public Class<?>[] groups() {
              return new Class<?>[0];
            }
            @Override
            public Class<? extends javax.validation.Payload>[] payload() {
              return new Class[0];
            }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
              return javax.validation.constraints.NotNull.class;
            }
          };
      when(descriptor.getAnnotation()).thenReturn(notNullAnnotation);

      return violation;
    }
}
