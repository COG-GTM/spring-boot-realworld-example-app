package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PasswordLengthValidatorTest {
  private static ValidatorFactory factory;
  private static Validator validator;

  private final PasswordLengthValidator passwordLengthValidator = new PasswordLengthValidator();

  @BeforeAll
  public static void setUpValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  public static void closeValidator() {
    factory.close();
  }

  @Test
  public void should_accept_null_password() {
    assertTrue(passwordLengthValidator.isValid(null, null));
  }

  @Test
  public void should_accept_empty_password() {
    assertTrue(passwordLengthValidator.isValid("", null));
  }

  @Test
  public void should_accept_ascii_password_of_exactly_72_bytes() {
    assertTrue(passwordLengthValidator.isValid("a".repeat(72), null));
  }

  @Test
  public void should_reject_ascii_password_of_73_bytes() {
    assertFalse(passwordLengthValidator.isValid("a".repeat(73), null));
  }

  @Test
  public void should_accept_multibyte_password_of_exactly_72_bytes() {
    assertTrue(passwordLengthValidator.isValid("\u00e9".repeat(36), null));
  }

  @Test
  public void should_count_utf8_bytes_not_characters() {
    String password = "\u00e9".repeat(37);
    assertEquals(37, password.length());
    assertFalse(passwordLengthValidator.isValid(password, null));
  }

  @Test
  public void should_reject_four_byte_characters_over_limit() {
    assertTrue(passwordLengthValidator.isValid("\uD83D\uDE00".repeat(18), null));
    assertFalse(passwordLengthValidator.isValid("\uD83D\uDE00".repeat(19), null));
  }

  @Test
  public void should_report_password_too_long_on_register_param() {
    RegisterParam param = new RegisterParam("john@jacob.com", "johnjacob", "a".repeat(73));

    Set<ConstraintViolation<RegisterParam>> violations =
        validator.validateProperty(param, "password");

    assertEquals(1, violations.size());
    ConstraintViolation<RegisterParam> violation = violations.iterator().next();
    assertEquals("password", violation.getPropertyPath().toString());
    assertEquals("is too long (maximum is 72 bytes)", violation.getMessage());
  }

  @Test
  public void should_accept_register_param_with_72_byte_password() {
    RegisterParam param = new RegisterParam("john@jacob.com", "johnjacob", "a".repeat(72));

    assertTrue(validator.validateProperty(param, "password").isEmpty());
  }

  @Test
  public void should_only_report_blank_error_for_empty_password() {
    RegisterParam param = new RegisterParam("john@jacob.com", "johnjacob", "");

    Set<ConstraintViolation<RegisterParam>> violations =
        validator.validateProperty(param, "password");

    assertEquals(1, violations.size());
    assertEquals("can't be empty", violations.iterator().next().getMessage());
  }
}
