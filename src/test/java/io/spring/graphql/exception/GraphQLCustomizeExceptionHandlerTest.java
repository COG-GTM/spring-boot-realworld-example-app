package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.graphql.types.Error;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void should_get_errors_as_data_with_dotted_path() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn((Class) String.class);
    when(violation.getMessage()).thenReturn("must not be blank");

    Path path = mock(Path.class);
    when(path.toString()).thenReturn("param.field.email");
    when(violation.getPropertyPath()).thenReturn(path);

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) Override.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    violations.add(violation);
    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);
    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertFalse(error.getErrors().isEmpty());
    assertEquals("email", error.getErrors().get(0).getKey());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void should_get_errors_as_data_with_single_segment_path() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn((Class) String.class);
    when(violation.getMessage()).thenReturn("invalid");

    Path path = mock(Path.class);
    when(path.toString()).thenReturn("singleField");
    when(violation.getPropertyPath()).thenReturn(path);

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) Override.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    violations.add(violation);
    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);
    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals("singleField", error.getErrors().get(0).getKey());
  }

  @Test
  public void should_create_authentication_exception() {
    AuthenticationException ex = new AuthenticationException();
    assertNotNull(ex);
    assertTrue(ex instanceof RuntimeException);
  }
}
