package io.spring.api.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

public class CustomizeExceptionHandlerTest {

  private MockMvc mvc;

  @BeforeEach
  public void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new CustomizeExceptionHandler())
            .build();
  }

  @Test
  public void should_return_422_with_field_errors_for_invalid_request() throws Exception {
    mvc.perform(get("/test/invalid-request"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.email").value(Matchers.contains("can't be empty")));
  }

  @Test
  public void should_return_422_with_property_path_for_constraint_violation() throws Exception {
    mvc.perform(get("/test/constraint-violation"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name").value(Matchers.contains("must not be blank")));
  }

  @Test
  public void should_return_422_when_request_body_is_not_valid() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "");

    mvc.perform(
            post("/test/valid-body")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name").isArray());
  }

  @Test
  public void should_accept_a_valid_request_body() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "jake");

    mvc.perform(
            post("/test/valid-body")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)))
        .andExpect(status().isOk());
  }

  @Test
  public void should_return_422_with_message_for_invalid_authentication() throws Exception {
    mvc.perform(get("/test/invalid-authentication"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("invalid email or password"));
  }

  @Test
  public void should_return_404_for_resource_not_found() throws Exception {
    mvc.perform(get("/test/not-found")).andExpect(status().isNotFound());
  }

  @Test
  public void should_return_403_for_no_authorization() throws Exception {
    mvc.perform(get("/test/no-authorization")).andExpect(status().isForbidden());
  }

  @RestController
  public static class ThrowingController {

    @GetMapping("/test/invalid-request")
    public void invalidRequest() {
      Errors errors = new BeanPropertyBindingResult(new Payload(), "payload");
      errors.rejectValue("email", "NotBlank", "can't be empty");
      throw new InvalidRequestException(errors);
    }

    @GetMapping("/test/constraint-violation")
    public void constraintViolation() {
      Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload());
      throw new ConstraintViolationException(violations);
    }

    @PostMapping("/test/valid-body")
    public String validBody(@Valid @RequestBody Payload payload) {
      return payload.getName();
    }

    @GetMapping("/test/invalid-authentication")
    public void invalidAuthentication() {
      throw new InvalidAuthenticationException();
    }

    @GetMapping("/test/not-found")
    public void notFound() {
      throw new ResourceNotFoundException();
    }

    @GetMapping("/test/no-authorization")
    public void noAuthorization() {
      throw new NoAuthorizationException();
    }
  }

  public static class Payload {
    @NotBlank private String name;

    private String email;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
