package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
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
    when(userRepository.findByEmail("free@example.com")).thenReturn(Optional.empty());

    assertThat(validator.isValid("free@example.com", null)).isTrue();
  }

  @Test
  public void should_be_invalid_when_email_already_exists() {
    User existing = new User("taken@example.com", "user", "pass", "", "");
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

    assertThat(validator.isValid("taken@example.com", null)).isFalse();
  }

  @Test
  public void should_be_valid_when_value_is_null() {
    assertThat(validator.isValid(null, null)).isTrue();
  }

  @Test
  public void should_be_valid_when_value_is_empty() {
    assertThat(validator.isValid("", null)).isTrue();
  }
}
