package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;
  private DuplicatedEmailValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new DuplicatedEmailValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  public void should_be_valid_when_email_is_null() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  public void should_be_valid_when_email_is_empty() {
    assertTrue(validator.isValid("", null));
  }

  @Test
  public void should_be_valid_when_email_is_not_taken() {
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
    assertTrue(validator.isValid("a@b.com", null));
  }

  @Test
  public void should_be_invalid_when_email_is_taken() {
    User existing = new User("a@b.com", "alice", "secret", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
    assertFalse(validator.isValid("a@b.com", null));
  }
}
