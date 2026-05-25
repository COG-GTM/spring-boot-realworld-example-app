package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedUsernameValidator validator;

  @Mock private ConstraintValidatorContext context;

  @Test
  void should_be_valid_for_new_username() {
    when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

    assertTrue(validator.isValid("newuser", context));
  }

  @Test
  void should_be_invalid_for_existing_username() {
    User existingUser = mock(User.class);
    when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

    assertFalse(validator.isValid("existinguser", context));
  }

  @Test
  void should_be_valid_for_null_username() {
    assertTrue(validator.isValid(null, context));
  }

  @Test
  void should_be_valid_for_empty_username() {
    assertTrue(validator.isValid("", context));
  }
}
