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

@Import({DuplicatedUsernameValidator.class, MyBatisUserRepository.class})
public class DuplicatedUsernameValidatorTest extends DbTestBase {
  @Autowired private DuplicatedUsernameValidator validator;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    User existingUser = new User("existing@test.com", "existinguser", "password", "", "");
    userRepository.save(existingUser);
  }

  @Test
  public void should_return_true_for_non_existing_username() {
    boolean isValid = validator.isValid("newuser", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_false_for_existing_username() {
    boolean isValid = validator.isValid("existinguser", null);
    Assertions.assertFalse(isValid);
  }

  @Test
  public void should_return_true_for_null_username() {
    boolean isValid = validator.isValid(null, null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_true_for_empty_username() {
    boolean isValid = validator.isValid("", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_be_case_sensitive_for_username() {
    boolean isValid = validator.isValid("EXISTINGUSER", null);
    Assertions.assertTrue(isValid);
  }

  @Test
  public void should_return_true_for_different_username() {
    boolean isValid = validator.isValid("differentuser", null);
    Assertions.assertTrue(isValid);
  }
}
