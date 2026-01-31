package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({DuplicatedEmailValidator.class, MyBatisUserRepository.class})
public class DuplicatedEmailValidatorTest extends DbTestBase {
  @Autowired private DuplicatedEmailValidator validator;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    User existingUser = new User("existing@test.com", "existinguser", "password", "", "");
    userRepository.save(existingUser);
  }

  @Test
  public void should_return_true_for_non_existing_email() {
    boolean isValid = validator.isValid("new@test.com", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_false_for_existing_email() {
    boolean isValid = validator.isValid("existing@test.com", null);
    Assertions.assertFalse(isValid);
  }

  @Test
  public void should_return_true_for_null_email() {
    boolean isValid = validator.isValid(null, null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_true_for_empty_email() {
    boolean isValid = validator.isValid("", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_be_case_sensitive_for_email() {
    boolean isValid = validator.isValid("EXISTING@test.com", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_true_for_different_email() {
    boolean isValid = validator.isValid("different@test.com", null);
    Assertions.assertTrue(isValid);
  }
}
