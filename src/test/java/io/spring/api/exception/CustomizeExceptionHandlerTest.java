package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

class CustomizeExceptionHandlerTest {

  private final CustomizeExceptionHandler handler = new CustomizeExceptionHandler();

  private WebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  private Errors errorsWithRejectedTitle() {
    TargetBean bean = new TargetBean();
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(bean, "targetBean");
    bindingResult.rejectValue("title", "NotBlank", "can't be empty");
    return bindingResult;
  }

  @Test
  void should_handle_invalid_request_with_unprocessable_entity_and_json_content_type() {
    InvalidRequestException exception = new InvalidRequestException(errorsWithRejectedTitle());

    var response = handler.handleInvalidRequest(exception, webRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/json");
    assertThat(response.getBody()).isInstanceOf(ErrorResource.class);
    List<FieldErrorResource> fieldErrors = ((ErrorResource) response.getBody()).getFieldErrors();
    assertThat(fieldErrors).hasSize(1);
    assertThat(fieldErrors.get(0).getResource()).isEqualTo("targetBean");
    assertThat(fieldErrors.get(0).getField()).isEqualTo("title");
    assertThat(fieldErrors.get(0).getCode()).isEqualTo("NotBlank");
    assertThat(fieldErrors.get(0).getMessage()).isEqualTo("can't be empty");
  }

  @Test
  void should_handle_invalid_authentication_with_message_body() {
    var response =
        handler.handleInvalidAuthentication(new InvalidAuthenticationException(), webRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).containsEntry("message", "invalid email or password");
  }

  @Test
  void should_handle_method_argument_not_valid_as_error_resource() throws Exception {
    MethodParameter parameter =
        new MethodParameter(TargetBean.class.getDeclaredMethod("handle", TargetBean.class), 0);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(
            parameter, (BeanPropertyBindingResult) errorsWithRejectedTitle());

    var response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    List<FieldErrorResource> fieldErrors = ((ErrorResource) response.getBody()).getFieldErrors();
    assertThat(fieldErrors).hasSize(1);
    assertThat(fieldErrors.get(0).getField()).isEqualTo("title");
    assertThat(fieldErrors.get(0).getCode()).isEqualTo("NotBlank");
  }

  @Test
  void should_handle_constraint_violation_with_simple_property_path() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    TargetBean bean = new TargetBean();
    Set<ConstraintViolation<TargetBean>> violations = validator.validate(bean);
    assertThat(violations).hasSize(1);

    ErrorResource errorResource =
        handler.handleConstraintViolation(new ConstraintViolationException(violations), null);

    assertThat(errorResource.getFieldErrors()).hasSize(1);
    FieldErrorResource fieldError = errorResource.getFieldErrors().get(0);
    assertThat(fieldError.getResource()).isEqualTo(TargetBean.class.getName());
    assertThat(fieldError.getField()).isEqualTo("title");
    assertThat(fieldError.getCode()).isEqualTo("NotBlank");
    assertThat(fieldError.getMessage()).isNotEmpty();
  }

  @Test
  void should_strip_method_prefix_from_nested_constraint_violation_property_path()
      throws Exception {
    Annotation notBlank = TargetBean.class.getDeclaredField("title").getAnnotation(NotBlank.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    when(descriptor.getAnnotation()).thenAnswer(invocation -> notBlank);

    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenAnswer(invocation -> TargetBean.class);
    when(violation.getPropertyPath()).thenReturn(new StaticPath("handle.targetBean.title"));
    when(violation.getMessage()).thenReturn("must not be blank");
    when(violation.getConstraintDescriptor()).thenAnswer(invocation -> descriptor);

    ErrorResource errorResource =
        handler.handleConstraintViolation(
            new ConstraintViolationException(Collections.singleton(violation)), null);

    FieldErrorResource fieldError = errorResource.getFieldErrors().get(0);
    assertThat(fieldError.getField()).isEqualTo("title");
    assertThat(fieldError.getCode()).isEqualTo("NotBlank");
    assertThat(fieldError.getMessage()).isEqualTo("must not be blank");
    assertThat(fieldError.getResource()).isEqualTo(TargetBean.class.getName());
  }

  private static class StaticPath implements Path {
    private final String value;

    StaticPath(String value) {
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

  public static class TargetBean {
    @NotBlank private String title;

    public String getTitle() {
      return title;
    }

    @SuppressWarnings("unused")
    void handle(TargetBean bean) {}
  }
}
