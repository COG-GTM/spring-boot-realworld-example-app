package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ResponseStatus;

class ExceptionTypesTest {

  @Test
  void should_keep_errors_and_empty_message_in_invalid_request_exception() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "target");
    errors.rejectValue(null, "invalid", "invalid value");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage()).isEmpty();
    assertThat(exception.getErrors()).isSameAs(errors);
    assertThat(exception.getErrors().getAllErrors()).hasSize(1);
  }

  @Test
  void should_use_default_message_for_invalid_authentication_exception() {
    Throwable thrown =
        catchThrowable(
            () -> {
              throw new InvalidAuthenticationException();
            });

    assertThat(thrown)
        .isInstanceOf(InvalidAuthenticationException.class)
        .hasMessage("invalid email or password");
  }

  @Test
  void should_map_no_authorization_exception_to_forbidden() {
    assertThat(new NoAuthorizationException()).isInstanceOf(RuntimeException.class);
    assertThat(NoAuthorizationException.class.getAnnotation(ResponseStatus.class).value())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void should_map_resource_not_found_exception_to_not_found() {
    assertThat(new ResourceNotFoundException()).isInstanceOf(RuntimeException.class);
    assertThat(ResourceNotFoundException.class.getAnnotation(ResponseStatus.class).value())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void should_expose_all_properties_of_field_error_resource() {
    FieldErrorResource fieldErrorResource =
        new FieldErrorResource("user", "email", "NotBlank", "can't be empty");

    assertThat(fieldErrorResource.getResource()).isEqualTo("user");
    assertThat(fieldErrorResource.getField()).isEqualTo("email");
    assertThat(fieldErrorResource.getCode()).isEqualTo("NotBlank");
    assertThat(fieldErrorResource.getMessage()).isEqualTo("can't be empty");
  }

  @Test
  void should_expose_field_errors_of_error_resource() {
    FieldErrorResource fieldErrorResource =
        new FieldErrorResource("user", "email", "NotBlank", "can't be empty");

    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldErrorResource));

    assertThat(errorResource.getFieldErrors()).containsExactly(fieldErrorResource);
  }
}
