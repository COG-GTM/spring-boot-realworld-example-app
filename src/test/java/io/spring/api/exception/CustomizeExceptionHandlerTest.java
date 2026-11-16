package io.spring.api.exception;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.MapBindingResult;
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
    mvc.perform(get("/invalid-request"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.title", contains("can't be empty")))
        .andExpect(
            jsonPath("$.errors.email", containsInAnyOrder("duplicated email", "should be email")));
  }

  @Test
  public void should_return_422_with_message_for_invalid_authentication() throws Exception {
    mvc.perform(get("/invalid-authentication"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("invalid email or password"));
  }

  @Test
  public void should_return_422_with_field_errors_for_invalid_request_body() throws Exception {
    mvc.perform(
            post("/validated-body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"email\":\"not-an-email\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.title", contains("can't be empty")))
        .andExpect(jsonPath("$.errors.email", contains("should be an email")));
  }

  @Test
  public void should_accept_valid_request_body() throws Exception {
    mvc.perform(
            post("/validated-body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"hello\",\"email\":\"a@b.com\"}"))
        .andExpect(status().isOk());
  }

  @Test
  public void should_return_422_with_param_path_for_constraint_violation() throws Exception {
    mvc.perform(get("/constraint-violation"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.title", contains("can't be empty")))
        .andExpect(jsonPath("$.errors.email", contains("should be an email")))
        .andExpect(jsonPath("$.errors.*", hasSize(2)));
  }

  @Test
  public void should_return_404_for_resource_not_found() throws Exception {
    mvc.perform(get("/not-found")).andExpect(status().isNotFound());
  }

  @Test
  public void should_return_403_for_no_authorization() throws Exception {
    mvc.perform(get("/forbidden")).andExpect(status().isForbidden());
  }

  public static class Payload {
    @NotBlank(message = "can't be empty")
    private String title;

    @Email(message = "should be an email")
    private String email;

    public Payload() {}

    public Payload(String title, String email) {
      this.title = title;
      this.email = email;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }

  public static class Target {
    public void create(@Valid Payload payload) {}
  }

  @RestController
  public static class ThrowingController {

    @GetMapping("/invalid-request")
    public void invalidRequest() {
      MapBindingResult errors = new MapBindingResult(new HashMap<>(), "article");
      errors.rejectValue("title", "NotBlank", "can't be empty");
      errors.rejectValue("email", "Duplicated", "duplicated email");
      errors.rejectValue("email", "Email", "should be email");
      throw new InvalidRequestException(errors);
    }

    @GetMapping("/invalid-authentication")
    public void invalidAuthentication() {
      throw new InvalidAuthenticationException();
    }

    @PostMapping("/validated-body")
    public String validatedBody(@Valid @RequestBody Payload payload) {
      return "ok";
    }

    @GetMapping("/constraint-violation")
    public void constraintViolation() throws NoSuchMethodException {
      Set<ConstraintViolation<Target>> violations =
          Validation.buildDefaultValidatorFactory()
              .getValidator()
              .forExecutables()
              .validateParameters(
                  new Target(),
                  Target.class.getMethod("create", Payload.class),
                  new Object[] {new Payload("", "not-an-email")});
      throw new ConstraintViolationException(violations);
    }

    @GetMapping("/not-found")
    public void notFound() {
      throw new ResourceNotFoundException();
    }

    @GetMapping("/forbidden")
    public void forbidden() {
      throw new NoAuthorizationException();
    }
  }
}
