package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
  public void should_be_valid_when_email_not_taken() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

    assertTrue(validator.isValid("new@example.com", null));
  }

  @Test
  public void should_be_invalid_when_email_taken() {
    when(userRepository.findByEmail("taken@example.com"))
        .thenReturn(Optional.of(new User("taken@example.com", "taken", "123", "", "")));

    assertFalse(validator.isValid("taken@example.com", null));
  }

  @Test
  public void should_be_valid_for_null_or_empty_without_querying_repository() {
    assertTrue(validator.isValid(null, null));
    assertTrue(validator.isValid("", null));

    verify(userRepository, never()).findByEmail(anyString());
  }
}
