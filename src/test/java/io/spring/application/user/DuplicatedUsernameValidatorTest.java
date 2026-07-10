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
public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedUsernameValidator validator;

  @Test
  public void should_be_valid_when_username_is_not_used() {
    when(userRepository.findByUsername("freename")).thenReturn(Optional.empty());

    assertTrue(validator.isValid("freename", null));
  }

  @Test
  public void should_be_invalid_when_username_is_already_used() {
    User existing = new User("someone@example.com", "usedname", "123", "", "");
    when(userRepository.findByUsername("usedname")).thenReturn(Optional.of(existing));

    assertFalse(validator.isValid("usedname", null));
  }

  @Test
  public void should_be_valid_when_username_is_null_or_empty() {
    assertTrue(validator.isValid(null, null));
    assertTrue(validator.isValid("", null));
  }
}
