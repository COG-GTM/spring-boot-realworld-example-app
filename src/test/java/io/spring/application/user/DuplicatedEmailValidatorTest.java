package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
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
  public void setUp() {
    validator = new DuplicatedEmailValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  public void should_be_valid_when_no_user_has_the_email() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

    assertThat(validator.isValid("john@example.com", context)).isTrue();
  }

  @Test
  public void should_be_invalid_when_a_user_already_has_the_email() {
    when(userRepository.findByEmail("john@example.com"))
        .thenReturn(Optional.of(new User("john@example.com", "john", "123", "", "")));

    assertThat(validator.isValid("john@example.com", context)).isFalse();
  }

  @Test
  public void should_skip_the_lookup_for_null_or_empty_email() {
    assertThat(validator.isValid(null, context)).isTrue();
    assertThat(validator.isValid("", context)).isTrue();

    verify(userRepository, never()).findByEmail(anyString());
  }
}
