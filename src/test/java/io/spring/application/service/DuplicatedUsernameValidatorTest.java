package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.lang.reflect.Constructor;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DuplicatedUsernameValidatorTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private ConstraintValidator<?, String> validator;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    Class<?> validatorClass =
        Class.forName("io.spring.application.user.DuplicatedUsernameValidator");
    Constructor<?> constructor = validatorClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    validator = (ConstraintValidator<?, String>) constructor.newInstance();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean isValid(String value) {
    return ((ConstraintValidator) validator).isValid(value, null);
  }

  @Test
  void should_be_valid_when_username_is_null() {
    assertThat(isValid(null)).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_username_is_empty() {
    assertThat(isValid("")).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_no_user_uses_the_username() {
    when(userRepository.findByUsername("jake")).thenReturn(Optional.empty());

    assertThat(isValid("jake")).isTrue();
  }

  @Test
  void should_be_invalid_when_a_user_already_uses_the_username() {
    when(userRepository.findByUsername("jake"))
        .thenReturn(Optional.of(new User("jake@jake.jake", "jake", "123", "bio", "image")));

    assertThat(isValid("jake")).isFalse();
  }
}
