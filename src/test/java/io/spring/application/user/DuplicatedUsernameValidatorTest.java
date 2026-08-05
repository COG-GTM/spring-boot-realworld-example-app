package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DuplicatedUsernameValidatorTest {

  @Mock private UserRepository userRepository;

  @Mock private ConstraintValidatorContext context;

  @InjectMocks private DuplicatedUsernameValidator validator;

  @Test
  public void should_be_valid_when_username_is_free() {
    when(userRepository.findByUsername("aisensiy")).thenReturn(Optional.empty());

    assertThat(validator.isValid("aisensiy", context)).isTrue();
  }

  @Test
  public void should_be_invalid_when_username_is_taken() {
    when(userRepository.findByUsername("aisensiy"))
        .thenReturn(Optional.of(new User("aisensiy@gmail.com", "aisensiy", "123", "", "")));

    assertThat(validator.isValid("aisensiy", context)).isFalse();
  }

  @Test
  public void should_be_valid_and_skip_lookup_when_username_is_null_or_empty() {
    assertThat(validator.isValid(null, context)).isTrue();
    assertThat(validator.isValid("", context)).isTrue();

    verifyNoInteractions(userRepository);
  }
}
