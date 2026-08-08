package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
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
public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  private DuplicatedUsernameValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new DuplicatedUsernameValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  public void should_be_valid_when_username_is_not_used() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

    assertThat(validator.isValid("john", null)).isTrue();
  }

  @Test
  public void should_be_invalid_when_username_is_already_used() {
    when(userRepository.findByUsername("john"))
        .thenReturn(Optional.of(new User("john@example.com", "john", "123", "", "")));

    assertThat(validator.isValid("john", null)).isFalse();
  }

  @Test
  public void should_short_circuit_for_null_or_empty_username() {
    assertThat(validator.isValid(null, null)).isTrue();
    assertThat(validator.isValid("", null)).isTrue();

    verifyNoInteractions(userRepository);
  }
}
