package io.spring.application.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordLengthValidator
    implements ConstraintValidator<PasswordLengthConstraint, String> {
  private static final int MAX_BCRYPT_BYTES = 72;

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES;
  }
}
