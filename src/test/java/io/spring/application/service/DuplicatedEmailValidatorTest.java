package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.application.user.DuplicatedEmailValidator;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DuplicatedEmailValidatorTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final DuplicatedEmailValidator validator = new DuplicatedEmailValidator();

  DuplicatedEmailValidatorTest() {
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  void should_be_valid_when_email_is_null() {
    assertThat(validator.isValid(null, null)).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_email_is_empty() {
    assertThat(validator.isValid("", null)).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_no_user_uses_the_email() {
    when(userRepository.findByEmail("jake@jake.jake")).thenReturn(Optional.empty());

    assertThat(validator.isValid("jake@jake.jake", null)).isTrue();
  }

  @Test
  void should_be_invalid_when_a_user_already_uses_the_email() {
    when(userRepository.findByEmail("jake@jake.jake"))
        .thenReturn(Optional.of(new User("jake@jake.jake", "jake", "123", "bio", "image")));

    assertThat(validator.isValid("jake@jake.jake", null)).isFalse();
  }
}
