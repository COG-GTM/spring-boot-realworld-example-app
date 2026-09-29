package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserValidatorsTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private UpdateUserValidator updateUserValidator;
  @InjectMocks private DuplicatedEmailValidator duplicatedEmailValidator;
  @InjectMocks private DuplicatedUsernameValidator duplicatedUsernameValidator;

  private ConstraintValidatorContext context;
  private User target;
  private User other;

  @BeforeEach
  public void setUp() {
    context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    target = new User("target@b.com", "target", "p", "", "");
    other = new User("other@b.com", "other", "p", "", "");
  }

  private UpdateUserCommand command(String email, String username) {
    return new UpdateUserCommand(
        target, UpdateUserParam.builder().email(email).username(username).build());
  }

  @Test
  public void update_should_be_valid_when_email_and_username_unused_or_owned() {
    when(userRepository.findByEmail("target@b.com")).thenReturn(Optional.of(target));
    when(userRepository.findByUsername("fresh")).thenReturn(Optional.empty());

    assertTrue(updateUserValidator.isValid(command("target@b.com", "fresh"), context));
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  public void update_should_report_both_email_and_username_taken() {
    when(userRepository.findByEmail("other@b.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    assertFalse(updateUserValidator.isValid(command("other@b.com", "other"), context));
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  @Test
  public void update_should_report_only_username_taken() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    assertFalse(updateUserValidator.isValid(command("new@b.com", "other"), context));
    verify(context, never()).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  @Test
  public void duplicated_email_validator_should_accept_blank_and_unused() {
    assertTrue(duplicatedEmailValidator.isValid(null, context));
    assertTrue(duplicatedEmailValidator.isValid("", context));
    when(userRepository.findByEmail("free@b.com")).thenReturn(Optional.empty());
    assertTrue(duplicatedEmailValidator.isValid("free@b.com", context));
    when(userRepository.findByEmail("other@b.com")).thenReturn(Optional.of(other));
    assertFalse(duplicatedEmailValidator.isValid("other@b.com", context));
  }

  @Test
  public void duplicated_username_validator_should_accept_blank_and_unused() {
    assertTrue(duplicatedUsernameValidator.isValid(null, context));
    assertTrue(duplicatedUsernameValidator.isValid("", context));
    when(userRepository.findByUsername(any())).thenReturn(Optional.of(other));
    assertFalse(duplicatedUsernameValidator.isValid("other", context));
  }
}
