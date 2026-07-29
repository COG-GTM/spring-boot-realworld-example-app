package io.spring.application.user;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedUsernameValidatorTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedUsernameValidator duplicatedUsernameValidator;

  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  public void should_be_valid_when_username_is_not_used() {
    when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

    Assertions.assertTrue(duplicatedUsernameValidator.isValid("new", context));
  }

  @Test
  public void should_be_invalid_when_username_is_already_used() {
    when(userRepository.findByUsername("used"))
        .thenReturn(Optional.of(new User("used@test.com", "used", "123", "", "")));

    Assertions.assertFalse(duplicatedUsernameValidator.isValid("used", context));
  }

  @Test
  public void should_be_valid_when_username_is_null_or_empty() {
    Assertions.assertTrue(duplicatedUsernameValidator.isValid(null, context));
    Assertions.assertTrue(duplicatedUsernameValidator.isValid("", context));

    verifyNoInteractions(userRepository);
  }
}
