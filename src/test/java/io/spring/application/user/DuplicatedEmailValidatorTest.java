package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedEmailValidator validator;

  @Test
  public void should_be_valid_when_email_is_not_used() {
    when(userRepository.findByEmail("free@example.com")).thenReturn(Optional.empty());

    assertTrue(validator.isValid("free@example.com", null));
  }

  @Test
  public void should_be_invalid_when_email_is_already_used() {
    User existing = new User("used@example.com", "someone", "123", "", "");
    when(userRepository.findByEmail("used@example.com")).thenReturn(Optional.of(existing));

    assertFalse(validator.isValid("used@example.com", null));
  }

  @Test
  public void should_be_valid_when_email_is_null_or_empty() {
    assertTrue(validator.isValid(null, null));
    assertTrue(validator.isValid("", null));
  }
}
