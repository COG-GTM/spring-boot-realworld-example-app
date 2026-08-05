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

public class DuplicatedEmailValidatorTest {

  private UserRepository userRepository;
  private ConstraintValidatorContext context;
  private DuplicatedEmailValidator validator;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    context = mock(ConstraintValidatorContext.class);
    validator = new DuplicatedEmailValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
  }

  @Test
  public void should_be_valid_when_no_user_with_email_exists() {
    when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());

    assertThat(validator.isValid("new@email.com", context), is(true));
    verify(userRepository).findByEmail("new@email.com");
  }

  @Test
  public void should_be_invalid_when_user_with_email_already_exists() {
    User existingUser = new User("taken@email.com", "username", "123", "bio", "image");
    when(userRepository.findByEmail("taken@email.com")).thenReturn(Optional.of(existingUser));

    assertThat(validator.isValid("taken@email.com", context), is(false));
    verify(userRepository).findByEmail("taken@email.com");
  }

  @Test
  public void should_be_valid_with_null_email_without_hitting_repository() {
    assertThat(validator.isValid(null, context), is(true));
    verify(userRepository, never()).findByEmail(anyString());
  }

  @Test
  public void should_be_valid_with_empty_email_without_hitting_repository() {
    assertThat(validator.isValid("", context), is(true));
    verify(userRepository, never()).findByEmail(anyString());
  }

  @Test
  public void should_look_up_blank_email_in_repository() {
    when(userRepository.findByEmail("   ")).thenReturn(Optional.empty());

    assertThat(validator.isValid("   ", context), is(true));
    verify(userRepository).findByEmail("   ");
  }

  @Test
  public void should_not_use_validator_context() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    validator.isValid("new@email.com", context);

    verify(context, never()).disableDefaultConstraintViolation();
    verify(context, never()).buildConstraintViolationWithTemplate(anyString());
  }
}
