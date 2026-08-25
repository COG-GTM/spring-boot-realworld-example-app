package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private static class Bean {
    @NotBlank(message = "can't be empty")
    private String title;

    public String getTitle() {
      return title;
    }
  }

  @SuppressWarnings("unused")
  private void createArticle(Bean bean) {}

  private final CustomizeExceptionHandler handler = new CustomizeExceptionHandler();
  private final WebRequest webRequest = mock(WebRequest.class);

  @Test
  public void should_translate_invalid_request_into_unprocessable_entity_with_field_errors() {
    Errors errors = new BeanPropertyBindingResult(new Bean(), "bean");
    errors.rejectValue("title", "EMPTY", "can't be empty");

    ResponseEntity<Object> response =
        handler.handleInvalidRequest(new InvalidRequestException(errors), webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body.getFieldErrors())
        .extracting(FieldErrorResource::getField, FieldErrorResource::getMessage)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("title", "can't be empty"));
  }

  @Test
  public void should_translate_invalid_authentication_into_message_body() {
    ResponseEntity<Object> response =
        handler.handleInvalidAuthentication(new InvalidAuthenticationException(), webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).containsEntry("message", new InvalidAuthenticationException().getMessage());
  }

  @Test
  public void should_translate_method_argument_not_valid_into_unprocessable_entity()
      throws Exception {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Bean(), "bean");
    errors.rejectValue("title", "EMPTY", "can't be empty");
    MethodParameter parameter =
        new MethodParameter(
            CustomizeExceptionHandlerTest.class.getDeclaredMethod("createArticle", Bean.class), 0);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, errors);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body.getFieldErrors())
        .extracting(FieldErrorResource::getField, FieldErrorResource::getMessage)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("title", "can't be empty"));
  }

  @Test
  public void should_use_plain_property_path_of_a_constraint_violation() throws Exception {
    ErrorResource errorResource =
        handler.handleConstraintViolation(
            new ConstraintViolationException(violations("title")), webRequest);

    assertThat(errorResource.getFieldErrors())
        .extracting(
            FieldErrorResource::getResource,
            FieldErrorResource::getField,
            FieldErrorResource::getCode,
            FieldErrorResource::getMessage)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                Bean.class.getName(), "title", "NotBlank", "can't be empty"));
  }

  @Test
  public void should_strip_method_and_argument_prefix_from_nested_property_path() throws Exception {
    ErrorResource errorResource =
        handler.handleConstraintViolation(
            new ConstraintViolationException(violations("createArticle.arg0.title")), webRequest);

    assertThat(errorResource.getFieldErrors())
        .extracting(FieldErrorResource::getField)
        .containsExactly("title");
  }

  private Set<ConstraintViolation<?>> violations(String propertyPath) throws Exception {
    NotBlank notBlank = Bean.class.getDeclaredField("title").getAnnotation(NotBlank.class);

    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    org.mockito.Mockito.doReturn(notBlank).when(descriptor).getAnnotation();

    javax.validation.Path path = mock(javax.validation.Path.class);
    when(path.toString()).thenReturn(propertyPath);

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    org.mockito.Mockito.doReturn(Bean.class).when(violation).getRootBeanClass();
    org.mockito.Mockito.doReturn(path).when(violation).getPropertyPath();
    org.mockito.Mockito.doReturn(descriptor).when(violation).getConstraintDescriptor();
    when(violation.getMessage()).thenReturn("can't be empty");

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    return violations;
  }
}
