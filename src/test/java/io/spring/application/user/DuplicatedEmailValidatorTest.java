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
public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedEmailValidator validator;

  @Mock private ConstraintValidatorContext context;

  @Test
  void should_be_valid_for_new_email() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

    assertTrue(validator.isValid("new@example.com", context));
  }

  @Test
  void should_be_invalid_for_existing_email() {
    User existingUser = mock(User.class);
    when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

    assertFalse(validator.isValid("existing@example.com", context));
  }

  @Test
  void should_be_valid_for_null_email() {
    assertTrue(validator.isValid(null, context));
  }

  @Test
  void should_be_valid_for_empty_email() {
    assertTrue(validator.isValid("", context));
  }
}
