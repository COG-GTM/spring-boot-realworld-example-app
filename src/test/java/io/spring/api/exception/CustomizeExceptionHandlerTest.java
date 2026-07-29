package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;
  private MockMvc mvc;

  @BeforeEach
  public void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = new ServletWebRequest(new MockHttpServletRequest());
    mvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new CustomizeExceptionHandler())
            .build();
  }

  @Test
  public void should_map_invalid_request_to_422_with_field_errors() {
    Errors errors = new BeanPropertyBindingResult(new RegisterFixture(), "registerParam");
    errors.rejectValue("email", "INVALID", "should be an email");
    errors.rejectValue("username", "DUPLICATED", "is already taken");

    ResponseEntity<Object> response =
        handler.handleInvalidRequest(new InvalidRequestException(errors), webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getFieldErrors())
        .extracting(
            FieldErrorResource::getResource,
            FieldErrorResource::getField,
            FieldErrorResource::getCode,
            FieldErrorResource::getMessage)
        .containsExactly(
            tuple("registerParam", "email", "INVALID", "should be an email"),
            tuple("registerParam", "username", "DUPLICATED", "is already taken"));
  }

  @Test
  public void should_keep_the_errors_available_on_the_invalid_request_exception() {
    Errors errors = new BeanPropertyBindingResult(new RegisterFixture(), "registerParam");
    errors.rejectValue("email", "INVALID", "should be an email");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertThat(exception.getErrors()).isSameAs(errors);
    assertThat(exception.getMessage()).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_map_invalid_authentication_to_422_with_a_message() {
    ResponseEntity<Object> response =
        handler.handleInvalidAuthentication(new InvalidAuthenticationException(), webRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).isInstanceOf(Map.class);
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).containsExactly(entry("message", "invalid email or password"));
  }

  @Test
  public void should_map_constraint_violations_of_a_bean_to_field_errors() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<RegisterFixture>> violations =
        validator.validate(new RegisterFixture());

    ErrorResource errorResource =
        handler.handleConstraintViolation(new ConstraintViolationException(violations), webRequest);

    assertThat(errorResource.getFieldErrors())
        .extracting(FieldErrorResource::getField, FieldErrorResource::getCode)
        .containsExactlyInAnyOrder(tuple("email", "NotBlank"), tuple("username", "NotBlank"));
    assertThat(errorResource.getFieldErrors())
        .allSatisfy(
            error -> assertThat(error.getResource()).isEqualTo(RegisterFixture.class.getName()));
  }

  @Test
  public void should_strip_the_method_prefix_of_a_method_validation_path() throws Exception {
    ExecutableValidator executableValidator =
        Validation.buildDefaultValidatorFactory().getValidator().forExecutables();
    TestController target = new TestController();
    Method method = TestController.class.getMethod("validated", RegisterFixture.class);
    Set<ConstraintViolation<TestController>> violations =
        executableValidator.validateParameters(
            target, method, new Object[] {new RegisterFixture()});

    ErrorResource errorResource =
        handler.handleConstraintViolation(new ConstraintViolationException(violations), webRequest);

    assertThat(errorResource.getFieldErrors())
        .extracting(FieldErrorResource::getField)
        .containsExactlyInAnyOrder("email", "username");
  }

  @Test
  public void should_render_invalid_request_through_mvc_as_realworld_error_body() throws Exception {
    mvc.perform(get("/invalid-request"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.email[0]").value("should be an email"))
        .andExpect(jsonPath("$.errors.username[0]").value("is already taken"));
  }

  @Test
  public void should_render_bean_validation_failures_as_realworld_error_body() throws Exception {
    mvc.perform(
            post("/validated-body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"username\":\"\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.email").isArray())
        .andExpect(jsonPath("$.errors.username").isArray());
  }

  @Test
  public void should_render_invalid_authentication_through_mvc() throws Exception {
    mvc.perform(get("/invalid-authentication"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("invalid email or password"));
  }

  @Test
  public void should_map_resource_not_found_to_404() throws Exception {
    mvc.perform(get("/not-found")).andExpect(status().isNotFound());
  }

  @Test
  public void should_map_no_authorization_to_403() throws Exception {
    mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
  }

  static class RegisterFixture {
    @NotBlank
    @Email(message = "should be an email")
    private String email = "";

    @NotBlank private String username = "";

    public String getEmail() {
      return email;
    }

    public String getUsername() {
      return username;
    }
  }

  @RestController
  static class TestController {

    @GetMapping("/invalid-request")
    public String invalidRequest() {
      Errors errors = new BeanPropertyBindingResult(new RegisterFixture(), "registerParam");
      errors.rejectValue("email", "INVALID", "should be an email");
      errors.rejectValue("username", "DUPLICATED", "is already taken");
      throw new InvalidRequestException(errors);
    }

    @GetMapping("/invalid-authentication")
    public String invalidAuthentication() {
      throw new InvalidAuthenticationException();
    }

    @GetMapping("/not-found")
    public String notFound() {
      throw new ResourceNotFoundException();
    }

    @GetMapping("/forbidden")
    public String forbidden() {
      throw new NoAuthorizationException();
    }

    @PostMapping("/validated-body")
    public String validatedBody(@Valid @RequestBody RegisterFixture fixture) {
      return "ok";
    }

    public String validated(@Valid RegisterFixture fixture) {
      return "ok";
    }
  }
}
