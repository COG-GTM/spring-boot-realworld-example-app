package io.spring.graphql;

import java.lang.reflect.Method;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Builds real bean validation failures, so tests exercise the actual violation metadata. */
public final class ConstraintViolationFixture {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private ConstraintViolationFixture() {}

  /** Violations with a single path segment, as produced by validating a bean directly. */
  public static javax.validation.ConstraintViolationException beanViolations() {
    return new javax.validation.ConstraintViolationException(
        VALIDATOR.validate(new NewUser("", "not-an-email")));
  }

  /** Violations with a nested path, as produced by validating the parameters of a service call. */
  public static javax.validation.ConstraintViolationException methodParameterViolations() {
    try {
      Method method = Service.class.getMethod("register", NewUser.class);
      return new javax.validation.ConstraintViolationException(
          VALIDATOR
              .forExecutables()
              .validateParameters(
                  new Service(), method, new Object[] {new NewUser("", "not-an-email")}));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class NewUser {
    @NotBlank(message = "can't be empty")
    @Size(min = 3, message = "too short")
    private final String username;

    @Email(message = "should be an email")
    private final String email;

    public NewUser(String username, String email) {
      this.username = username;
      this.email = email;
    }

    public String getUsername() {
      return username;
    }

    public String getEmail() {
      return email;
    }
  }

  public static class Service {
    public void register(@Valid NewUser newUser) {}
  }
}
