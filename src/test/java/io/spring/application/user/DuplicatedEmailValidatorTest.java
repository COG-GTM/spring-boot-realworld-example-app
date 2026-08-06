package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DuplicatedEmailValidatorTest {

  @Mock private UserRepository userRepository;
  @Mock private ConstraintValidatorContext context;

  private DuplicatedEmailValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DuplicatedEmailValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  void should_be_valid_when_value_is_null() {
    assertThat(validator.isValid(null, context)).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_value_is_empty() {
    assertThat(validator.isValid("", context)).isTrue();
    verifyNoInteractions(userRepository);
  }

  @Test
  void should_be_valid_when_no_user_uses_the_email() {
    when(userRepository.findByEmail("free@test.com")).thenReturn(Optional.empty());

    assertThat(validator.isValid("free@test.com", context)).isTrue();
  }

  @Test
  void should_be_invalid_when_a_user_already_uses_the_email() {
    when(userRepository.findByEmail("john@test.com"))
        .thenReturn(Optional.of(TestHelper.userFixture("john")));

    assertThat(validator.isValid("john@test.com", context)).isFalse();
  }
}
