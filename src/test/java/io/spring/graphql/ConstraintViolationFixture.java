package io.spring.graphql;

import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Builds real {@link ConstraintViolationException}s so the graphql error mapping can be exercised
 * against actual bean-validation metadata instead of mocks.
 */
public final class ConstraintViolationFixture {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private ConstraintViolationFixture() {}

  /** Violations whose property path is a plain property name, e.g. {@code email}. */
  public static ConstraintViolationException propertyPathViolations() {
    Set<ConstraintViolation<RegistrationForm>> violations =
        VALIDATOR.validate(new RegistrationForm("", "a"));
    return new ConstraintViolationException(violations);
  }

  /** Violations whose property path is a method path, e.g. {@code register.arg0.email}. */
  public static ConstraintViolationException methodPathViolations() {
    try {
      Method method = Registrar.class.getMethod("register", RegistrationForm.class);
      Set<ConstraintViolation<Registrar>> violations =
          VALIDATOR
              .forExecutables()
              .validateParameters(
                  new Registrar(), method, new Object[] {new RegistrationForm("", "a")});
      return new ConstraintViolationException(violations);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class RegistrationForm {

    @NotBlank(message = "can't be empty")
    private final String username;

    @Email(message = "should be an email")
    @Size(min = 5, message = "should be at least 5 characters")
    private final String email;

    public RegistrationForm(String username, String email) {
      this.username = username;
      this.email = email;
    }
  }

  public static class Registrar {

    public void register(@Valid RegistrationForm form) {}
  }
}
