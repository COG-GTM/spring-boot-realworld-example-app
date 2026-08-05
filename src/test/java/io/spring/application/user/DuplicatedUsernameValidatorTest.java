package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DuplicatedUsernameValidatorTest {

  private UserRepository userRepository;
  private ConstraintValidatorContext context;
  private DuplicatedUsernameValidator validator;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    context = mock(ConstraintValidatorContext.class);
    validator = new DuplicatedUsernameValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  public void should_be_valid_when_no_user_with_username_exists() {
    when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

    assertThat(validator.isValid("newuser", context), is(true));
    verify(userRepository).findByUsername("newuser");
  }

  @Test
  public void should_be_invalid_when_user_with_username_already_exists() {
    User existingUser = new User("email@email.com", "takenuser", "123", "bio", "image");
    when(userRepository.findByUsername("takenuser")).thenReturn(Optional.of(existingUser));

    assertThat(validator.isValid("takenuser", context), is(false));
    verify(userRepository).findByUsername("takenuser");
  }

  @Test
  public void should_be_valid_with_null_username_without_hitting_repository() {
    assertThat(validator.isValid(null, context), is(true));
    verify(userRepository, never()).findByUsername(anyString());
  }

  @Test
  public void should_be_valid_with_empty_username_without_hitting_repository() {
    assertThat(validator.isValid("", context), is(true));
    verify(userRepository, never()).findByUsername(anyString());
  }

  @Test
  public void should_look_up_blank_username_in_repository() {
    when(userRepository.findByUsername("   ")).thenReturn(Optional.empty());

    assertThat(validator.isValid("   ", context), is(true));
    verify(userRepository).findByUsername("   ");
  }

  @Test
  public void should_not_use_validator_context() {
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

    validator.isValid("newuser", context);

    verify(context, never()).disableDefaultConstraintViolation();
    verify(context, never()).buildConstraintViolationWithTemplate(anyString());
  }
}
