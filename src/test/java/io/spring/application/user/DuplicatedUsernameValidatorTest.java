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
public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private DuplicatedUsernameValidator validator;

  @Test
  public void should_be_valid_when_username_not_taken() {
    when(userRepository.findByUsername("free")).thenReturn(Optional.empty());

    assertThat(validator.isValid("free", null)).isTrue();
  }

  @Test
  public void should_be_invalid_when_username_already_exists() {
    User existing = new User("user@example.com", "taken", "pass", "", "");
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existing));

    assertThat(validator.isValid("taken", null)).isFalse();
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
