package io.spring.graphql;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.application.data.UserData;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class UserMutationTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserService userService;

  @MockBean private UserRepository userRepository;

  @MockBean private UserReadService userReadService;

  @MockBean private JwtService jwtService;

  @MockBean private PasswordEncoder encryptService;

  protected User user;
  protected UserData userData;
  protected String token;
  protected String email;
  protected String username;
  protected String defaultAvatar;

  @BeforeEach
  public void setUp() throws Exception {
    RestAssuredMockMvc.mockMvc(mvc);
    userFixture();
  }

  protected void userFixture() {
    email = "john@jacob.com";
    username = "johnjacob";
    defaultAvatar = "https://static.productionready.io/images/smiley-cyrus.jpg";

    user = new User(email, username, "123", "", defaultAvatar);
    when(userRepository.findByUsername(eq(username))).thenReturn(Optional.of(user));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    userData = new UserData(user.getId(), email, username, "", defaultAvatar);
    when(userReadService.findById(eq(user.getId()))).thenReturn(userData);

    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(jwtService.toToken(any())).thenReturn(token);
  }

  @Test
  public void should_create_user_success() throws Exception {
    String testEmail = "newuser@example.com";
    String testUsername = "newuser";
    String testPassword = "password123";

    User newUser = new User(testEmail, testUsername, testPassword, "", "");
    when(userService.createUser(any())).thenReturn(newUser);

    String mutation =
        "mutation { createUser(input: {email: \\\""
            + testEmail
            + "\\\", username: \\\""
            + testUsername
            + "\\\", password: \\\""
            + testPassword
            + "\\\"}) { ... on UserPayload { user { email username token } } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.createUser.user.email", equalTo(testEmail))
        .body("data.createUser.user.username", equalTo(testUsername));
  }

  @Test
  public void should_return_error_for_invalid_email() throws Exception {
    String invalidEmail = "invalid-email";
    String testUsername = "newuser";
    String testPassword = "password123";

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "should be an email"));

    ConstraintViolationException cve = new ConstraintViolationException("Validation failed", violations);
    when(userService.createUser(any())).thenThrow(cve);

    String mutation =
        "mutation { createUser(input: {email: \\\""
            + invalidEmail
            + "\\\", username: \\\""
            + testUsername
            + "\\\", password: \\\""
            + testPassword
            + "\\\"}) { ... on Error { message errors { key value } } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.createUser.message", equalTo("BAD_REQUEST"));
  }

  @Test
  public void should_return_error_for_empty_username() throws Exception {
    String testEmail = "test@example.com";
    String emptyUsername = "";
    String testPassword = "password123";

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("username", "can't be empty"));

    ConstraintViolationException cve = new ConstraintViolationException("Validation failed", violations);
    when(userService.createUser(any())).thenThrow(cve);

    String mutation =
        "mutation { createUser(input: {email: \\\""
            + testEmail
            + "\\\", username: \\\""
            + emptyUsername
            + "\\\", password: \\\""
            + testPassword
            + "\\\"}) { ... on Error { message errors { key value } } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.createUser.message", equalTo("BAD_REQUEST"));
  }

  @Test
  public void should_login_success() throws Exception {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("123"), eq(user.getPassword()))).thenReturn(true);

    String mutation =
        "mutation { login(email: \\\"" + email + "\\\", password: \\\"123\\\") { user { email username token } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.login.user.email", equalTo(email))
        .body("data.login.user.username", equalTo(username));
  }

  @Test
  public void should_fail_login_with_wrong_password() throws Exception {
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));
    when(encryptService.matches(eq("wrongpassword"), eq(user.getPassword()))).thenReturn(false);

    String mutation =
        "mutation { login(email: \\\"" + email + "\\\", password: \\\"wrongpassword\\\") { user { email username token } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("UNAUTHENTICATED"));
  }

  @Test
  public void should_fail_login_with_nonexistent_user() throws Exception {
    when(userRepository.findByEmail(eq("nonexistent@example.com"))).thenReturn(Optional.empty());

    String mutation =
        "mutation { login(email: \\\"nonexistent@example.com\\\", password: \\\"password\\\") { user { email username token } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].extensions.errorType", equalTo("UNAUTHENTICATED"));
  }

  @Test
  public void should_update_user_success() throws Exception {
    String newBio = "Updated bio";

    // The updateUser mutation requires authentication via the JwtTokenFilter
    // which populates SecurityContextHolder with the authenticated user.
    // The MeDatafetcher.getUserPayloadUser method then uses the user from
    // localContext to build the response with email, username, and token.
    String mutation =
        "mutation { updateUser(changes: {bio: \\\"" + newBio + "\\\"}) { user { email username token } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors", equalTo(null))
        .body("data.updateUser.user.email", equalTo(email))
        .body("data.updateUser.user.username", equalTo(username))
        .body("data.updateUser.user.token", equalTo(token));

    verify(userService).updateUser(any(UpdateUserCommand.class));
  }

  @Test
  public void should_update_user_email_only() throws Exception {
    String newEmail = "newemail@example.com";

    String mutation =
        "mutation { updateUser(changes: {email: \\\"" + newEmail + "\\\"}) { user { email username } } }";

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_return_null_when_not_authenticated_for_update() throws Exception {
    String newBio = "Updated bio";

    String mutation =
        "mutation { updateUser(changes: {bio: \\\"" + newBio + "\\\"}) { user { email username bio } } }";

    given()
        .contentType("application/json")
        .body("{\"query\": \"" + mutation + "\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.updateUser", equalTo(null));
  }

  @SuppressWarnings("unchecked")
  private ConstraintViolation<?> createMockViolation(String field, String message) {
    return new ConstraintViolation<Object>() {
      @Override
      public String getMessage() {
        return message;
      }

      @Override
      public String getMessageTemplate() {
        return message;
      }

      @Override
      public Object getRootBean() {
        return null;
      }

      @Override
      public Class<Object> getRootBeanClass() {
        return Object.class;
      }

      @Override
      public Object getLeafBean() {
        return null;
      }

      @Override
      public Object[] getExecutableParameters() {
        return new Object[0];
      }

      @Override
      public Object getExecutableReturnValue() {
        return null;
      }

      @Override
      public Path getPropertyPath() {
        return new Path() {
          @Override
          public java.util.Iterator<Node> iterator() {
            return java.util.Collections.singletonList(
                    (Node)
                        new Path.Node() {
                          @Override
                          public String getName() {
                            return field;
                          }

                          @Override
                          public boolean isInIterable() {
                            return false;
                          }

                          @Override
                          public Integer getIndex() {
                            return null;
                          }

                          @Override
                          public Object getKey() {
                            return null;
                          }

                          @Override
                          public ElementKind getKind() {
                            return ElementKind.PROPERTY;
                          }

                          @Override
                          public <T extends Node> T as(Class<T> nodeType) {
                            return null;
                          }
                        })
                .iterator();
          }

          @Override
          public String toString() {
            return "param." + field;
          }
        };
      }

      @Override
      public Object getInvalidValue() {
        return null;
      }

      @Override
      public ConstraintDescriptor<?> getConstraintDescriptor() {
        return new ConstraintDescriptor<java.lang.annotation.Annotation>() {
          @Override
          public java.lang.annotation.Annotation getAnnotation() {
            return new javax.validation.constraints.NotBlank() {
              @Override
              public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return javax.validation.constraints.NotBlank.class;
              }

              @Override
              public String message() {
                return message;
              }

              @Override
              public Class<?>[] groups() {
                return new Class[0];
              }

              @Override
              public Class<? extends javax.validation.Payload>[] payload() {
                return new Class[0];
              }
            };
          }

          @Override
          public String getMessageTemplate() {
            return message;
          }

          @Override
          public Set<Class<?>> getGroups() {
            return new HashSet<>();
          }

          @Override
          public Set<Class<? extends javax.validation.Payload>> getPayload() {
            return new HashSet<>();
          }

          @Override
          public javax.validation.ConstraintTarget getValidationAppliesTo() {
            return null;
          }

          @Override
          public java.util.List<Class<? extends javax.validation.ConstraintValidator<java.lang.annotation.Annotation, ?>>>
              getConstraintValidatorClasses() {
            return new java.util.ArrayList<>();
          }

          @Override
          public java.util.Map<String, Object> getAttributes() {
            return new java.util.HashMap<>();
          }

          @Override
          public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            return new HashSet<>();
          }

          @Override
          public boolean isReportAsSingleViolation() {
            return false;
          }

          @Override
          public javax.validation.metadata.ValidateUnwrappedValue getValueUnwrapping() {
            return null;
          }

          @Override
          public <U> U unwrap(Class<U> type) {
            return null;
          }
        };
      }

      @Override
      public <U> U unwrap(Class<U> type) {
        return null;
      }
    };
  }
}
