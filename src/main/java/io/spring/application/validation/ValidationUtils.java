package io.spring.application.validation;

import java.util.Optional;
import java.util.function.Function;
import javax.validation.ConstraintValidatorContext;

public final class ValidationUtils {

  private ValidationUtils() {}

  public static boolean isNullOrEmpty(String value) {
    return value == null || value.trim().isEmpty();
  }

  public static boolean isValidLength(String value, int minLength, int maxLength) {
    if (value == null) {
      return false;
    }
    int length = value.length();
    return length >= minLength && length <= maxLength;
  }

  public static <T> boolean checkUniqueness(
      String value, Function<String, Optional<T>> findFunction) {
    if (isNullOrEmpty(value)) {
      return true;
    }
    return !findFunction.apply(value).isPresent();
  }

  public static void addConstraintViolation(
      ConstraintValidatorContext context, String propertyNode, String message) {
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(propertyNode)
        .addConstraintViolation();
  }

  public static boolean isValidEmail(String email) {
    if (isNullOrEmpty(email)) {
      return false;
    }
    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
  }

  public static boolean isValidUsername(String username) {
    if (isNullOrEmpty(username)) {
      return false;
    }
    return username.matches("^[a-zA-Z0-9_-]+$");
  }
}
