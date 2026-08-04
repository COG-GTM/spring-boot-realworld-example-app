package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public class CustomizeExceptionHandlerTest {

  private static final ValidatorFactory VALIDATOR_FACTORY =
      Validation.buildDefaultValidatorFactory();

  private MockMvc mvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new CustomizeExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @AfterAll
  public static void tearDown() {
    VALIDATOR_FACTORY.close();
  }

  @Test
  public void should_handle_invalid_request_exception_with_422_and_grouped_errors()
      throws Exception {
    ResultActions result =
        mvc.perform(get("/test/invalid-request"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

    assertJsonBody(
        "{\"errors\":{"
            + "\"email\":[\"can't be empty\",\"should be an email\"],"
            + "\"password\":[\"can't be empty\"]}}",
        result);
  }

  @Test
  public void should_handle_bean_validation_failure_with_422_and_grouped_errors() throws Exception {
    ResultActions result =
        mvc.perform(
                post("/test/validated")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
            .andExpect(status().isUnprocessableEntity());

    assertJsonBody(
        "{\"errors\":{"
            + "\"email\":[\"should be an email\"],"
            + "\"password\":[\"can't be empty\"]}}",
        result);
  }

  @Test
  public void should_handle_constraint_violation_of_a_bean_property_with_422() throws Exception {
    ResultActions result =
        mvc.perform(get("/test/bean-constraint-violation"))
            .andExpect(status().isUnprocessableEntity());

    assertJsonBody("{\"errors\":{\"password\":[\"can't be empty\"]}}", result);
  }

  @Test
  public void should_strip_method_and_parameter_from_constraint_violation_property_path()
      throws Exception {
    ResultActions result =
        mvc.perform(get("/test/method-constraint-violation"))
            .andExpect(status().isUnprocessableEntity());

    assertJsonBody("{\"errors\":{\"password\":[\"can't be empty\"]}}", result);
  }

  @Test
  public void should_handle_invalid_authentication_exception_with_422_and_message()
      throws Exception {
    ResultActions result =
        mvc.perform(get("/test/invalid-authentication"))
            .andExpect(status().isUnprocessableEntity());

    assertJsonBody("{\"message\":\"invalid email or password\"}", result);
  }

  @Test
  public void should_map_resource_not_found_exception_to_404() throws Exception {
    mvc.perform(get("/test/not-found")).andExpect(status().isNotFound());
  }

  @Test
  public void should_map_no_authorization_exception_to_403() throws Exception {
    mvc.perform(get("/test/forbidden")).andExpect(status().isForbidden());
  }

  private void assertJsonBody(String expected, ResultActions result) throws Exception {
    assertEquals(
        objectMapper.readTree(expected),
        objectMapper.readTree(result.andReturn().getResponse().getContentAsString()));
  }

  private static Set<ConstraintViolation<TestParam>> beanViolations(TestParam param) {
    return VALIDATOR_FACTORY.getValidator().validate(param);
  }

  private static Set<ConstraintViolation<TestService>> methodViolations(TestParam param)
      throws NoSuchMethodException {
    Validator validator = VALIDATOR_FACTORY.getValidator();
    ExecutableValidator executableValidator = validator.forExecutables();
    Method method = TestService.class.getMethod("update", TestParam.class);
    return executableValidator.validateParameters(new TestService(), method, new Object[] {param});
  }

  @RestController
  @RequestMapping("/test")
  static class TestController {

    @RequestMapping("/invalid-request")
    public void invalidRequest() {
      Errors errors = new BeanPropertyBindingResult(new TestParam(), "testParam");
      errors.rejectValue("email", "NotBlank", "can't be empty");
      errors.rejectValue("email", "Email", "should be an email");
      errors.rejectValue("password", "NotBlank", "can't be empty");
      throw new InvalidRequestException(errors);
    }

    @PostMapping("/validated")
    public void validated(@Valid @RequestBody TestParam param) {}

    @RequestMapping("/bean-constraint-violation")
    public void beanConstraintViolation() {
      throw new ConstraintViolationException(beanViolations(new TestParam("a@b.com", "")));
    }

    @RequestMapping("/method-constraint-violation")
    public void methodConstraintViolation() throws NoSuchMethodException {
      throw new ConstraintViolationException(methodViolations(new TestParam("a@b.com", "")));
    }

    @RequestMapping("/invalid-authentication")
    public void invalidAuthentication() {
      throw new InvalidAuthenticationException();
    }

    @RequestMapping("/not-found")
    public void notFound() {
      throw new ResourceNotFoundException();
    }

    @RequestMapping("/forbidden")
    public void forbidden() {
      throw new NoAuthorizationException();
    }
  }

  static class TestService {
    public void update(@Valid TestParam param) {}
  }

  static class TestParam {
    @Email(message = "should be an email")
    private String email;

    @NotBlank(message = "can't be empty")
    private String password;

    TestParam() {}

    TestParam(String email, String password) {
      this.email = email;
      this.password = password;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
