package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private final CustomizeExceptionHandler handler = new CustomizeExceptionHandler();

  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new ThrowingController())
          .setControllerAdvice(new CustomizeExceptionHandler())
          .build();

  private WebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  @Test
  public void should_handle_invalid_request_with_422_and_field_errors() {
    Errors errors = new BeanPropertyBindingResult(new TargetBean(), "targetBean");
    errors.rejectValue("name", "NotBlank", "can't be empty");

    ResponseEntity<Object> response =
        handler.handleInvalidRequest(new InvalidRequestException(errors), webRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/json");
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body.getFieldErrors()).hasSize(1);
    assertThat(body.getFieldErrors().get(0).getField()).isEqualTo("name");
    assertThat(body.getFieldErrors().get(0).getCode()).isEqualTo("NotBlank");
    assertThat(body.getFieldErrors().get(0).getMessage()).isEqualTo("can't be empty");
    assertThat(body.getFieldErrors().get(0).getResource()).isEqualTo("targetBean");
  }

  @Test
  public void should_handle_invalid_authentication_with_422_and_message() {
    ResponseEntity<Object> response =
        handler.handleInvalidAuthentication(new InvalidAuthenticationException(), webRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody().toString()).contains("invalid email or password");
  }

  @Test
  public void should_handle_constraint_violation_with_field_errors() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<TargetBean>> violations = validator.validate(new TargetBean());
    assertThat(violations).isNotEmpty();

    ErrorResource errorResource =
        handler.handleConstraintViolation(
            new ConstraintViolationException(violations), webRequest());

    assertThat(errorResource.getFieldErrors()).hasSize(1);
    FieldErrorResource fieldError = errorResource.getFieldErrors().get(0);
    assertThat(fieldError.getResource()).isEqualTo(TargetBean.class.getName());
    assertThat(fieldError.getField()).isEqualTo("name");
    assertThat(fieldError.getCode()).isEqualTo("NotBlank");
    assertThat(fieldError.getMessage()).isNotEmpty();
  }

  @Test
  public void should_return_422_for_constraint_violation_through_mvc() throws Exception {
    mockMvc
        .perform(get("/test/constraint-violation"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name[0]").value("must not be blank"));
  }

  @Test
  public void should_return_404_for_resource_not_found() throws Exception {
    mockMvc.perform(get("/test/not-found")).andExpect(status().isNotFound());
  }

  @Test
  public void should_return_403_for_no_authorization() throws Exception {
    mockMvc.perform(get("/test/no-authorization")).andExpect(status().isForbidden());
  }

  @Test
  public void should_return_422_with_field_errors_for_invalid_request_through_mvc()
      throws Exception {
    mockMvc
        .perform(get("/test/invalid-request"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name[0]").value("can't be empty"));
  }

  public static class TargetBean {
    @NotBlank private String name;

    public String getName() {
      return name;
    }
  }

  @RestController
  public static class ThrowingController {

    @GetMapping("/test/not-found")
    public String notFound() {
      throw new ResourceNotFoundException();
    }

    @GetMapping("/test/no-authorization")
    public String noAuthorization() {
      throw new NoAuthorizationException();
    }

    @GetMapping("/test/invalid-request")
    public String invalidRequest() {
      Errors errors = new BeanPropertyBindingResult(new TargetBean(), "targetBean");
      errors.rejectValue("name", "NotBlank", "can't be empty");
      throw new InvalidRequestException(errors);
    }

    @GetMapping("/test/constraint-violation")
    public String constraintViolation() {
      Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      Set<ConstraintViolation<TargetBean>> violations = validator.validate(new TargetBean());
      throw new ConstraintViolationException(violations);
    }
  }
}
