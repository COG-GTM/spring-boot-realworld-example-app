package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CustomizeExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CustomizeExceptionHandler.class, CustomizeExceptionHandlerTest.TestController.class})
public class CustomizeExceptionHandlerTest {
  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  @Test
  public void should_return_422_with_field_errors_for_invalid_request() throws Exception {
    mvc.perform(get("/exception-test/invalid-request"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errors.title[0]").value("can't be empty"));
  }

  @Test
  public void should_return_422_with_message_for_invalid_authentication() throws Exception {
    mvc.perform(get("/exception-test/invalid-authentication"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("invalid email or password"));
  }

  @Test
  public void should_return_422_for_request_body_validation_errors() throws Exception {
    mvc.perform(
            post("/exception-test/method-argument")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"request\":{\"name\":\"\"}}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name[0]").value("can't be empty"));
  }

  @Test
  public void should_return_422_for_constraint_violations() throws Exception {
    mvc.perform(get("/exception-test/constraint-violation"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.name[0]").value("can't be empty"));
  }

  @Test
  public void should_return_404_for_resource_not_found() throws Exception {
    mvc.perform(get("/exception-test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(ResourceNotFoundException.class));
  }

  @RestController
  @RequestMapping("/exception-test")
  public static class TestController {
    private final Validator validator;

    public TestController(Validator validator) {
      this.validator = validator;
    }

    @GetMapping("/invalid-request")
    public void invalidRequest() {
      BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "article");
      errors.addError(
          new FieldError(
              "article", "title", null, false, new String[] {"NotBlank"}, null, "can't be empty"));
      throw new InvalidRequestException(errors);
    }

    @GetMapping("/invalid-authentication")
    public void invalidAuthentication() {
      throw new InvalidAuthenticationException();
    }

    @PostMapping("/method-argument")
    public void methodArgument(@Valid @RequestBody ValidationRequest request) {}

    @GetMapping("/constraint-violation")
    public void constraintViolation() {
      Set<ConstraintViolation<ValidationRequest>> violations =
          validator.validate(new ValidationRequest(""));
      throw new ConstraintViolationException(violations);
    }

    @GetMapping("/not-found")
    public void notFound() {
      throw new ResourceNotFoundException();
    }
  }

  @JsonRootName("request")
  public static class ValidationRequest {
    @NotBlank(message = "can't be empty")
    private String name;

    public ValidationRequest() {}

    public ValidationRequest(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
