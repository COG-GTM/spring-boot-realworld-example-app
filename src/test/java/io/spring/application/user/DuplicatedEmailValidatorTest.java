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
public class DuplicatedEmailValidatorTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedEmailValidator duplicatedEmailValidator;

  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  public void should_be_valid_when_email_is_not_used() {
    when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

    Assertions.assertTrue(duplicatedEmailValidator.isValid("new@test.com", context));
  }

  @Test
  public void should_be_invalid_when_email_is_already_used() {
    when(userRepository.findByEmail("used@test.com"))
        .thenReturn(Optional.of(new User("used@test.com", "user", "123", "", "")));

    Assertions.assertFalse(duplicatedEmailValidator.isValid("used@test.com", context));
  }

  @Test
  public void should_be_valid_when_email_is_null_or_empty() {
    Assertions.assertTrue(duplicatedEmailValidator.isValid(null, context));
    Assertions.assertTrue(duplicatedEmailValidator.isValid("", context));

    verifyNoInteractions(userRepository);
  }
}
