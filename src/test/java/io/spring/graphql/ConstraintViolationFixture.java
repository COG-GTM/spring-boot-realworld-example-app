package io.spring.graphql;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;

/**
 * Builds a {@link ConstraintViolationException} for blank registration fields without the
 * Spring-injected duplicate-user validators declared on {@code RegisterParam}.
 */
public final class ConstraintViolationFixture {
  private ConstraintViolationFixture() {}

  public static ConstraintViolationException blankRegistration() {
    return new ConstraintViolationException(
        Validation.buildDefaultValidatorFactory().getValidator().validate(new BlankRegistration()));
  }

  static class BlankRegistration {
    @NotBlank(message = "can't be empty")
    private final String email = "";

    @NotBlank(message = "can't be empty")
    private final String username = "";

    @NotBlank(message = "can't be empty")
    private final String password = "";
  }
}
