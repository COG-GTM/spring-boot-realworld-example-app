package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
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

@ExtendWith(MockitoExtension.class)
public class UpdateUserCommandTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UpdateUserValidator validator;

  private User targetUser;

  @BeforeEach
  public void setUp() {
    targetUser = new User("target@example.com", "target", "123", "", "");
  }

  private UpdateUserCommand command(String email, String username) {
    UpdateUserParam param = UpdateUserParam.builder().email(email).username(username).build();
    return new UpdateUserCommand(targetUser, param);
  }

  @Test
  public void should_expose_target_user_and_param() {
    UpdateUserParam param = UpdateUserParam.builder().email("a@example.com").build();
    UpdateUserCommand cmd = new UpdateUserCommand(targetUser, param);

    assertSame(targetUser, cmd.getTargetUser());
    assertSame(param, cmd.getParam());
  }

  @Test
  public void should_be_valid_when_email_and_username_are_unused() {
    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    assertTrue(validator.isValid(command("new@example.com", "newname"), context));
  }

  @Test
  public void should_be_valid_when_email_and_username_belong_to_target_user() {
    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
    when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));

    assertTrue(validator.isValid(command("target@example.com", "target"), context));
  }

  @Test
  public void should_be_invalid_when_email_belongs_to_another_user() {
    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    User other = new User("other@example.com", "other", "123", "", "");
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    assertFalse(validator.isValid(command("other@example.com", "newname"), context));
  }

  @Test
  public void should_be_invalid_when_username_belongs_to_another_user() {
    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
    User other = new User("other@example.com", "other", "123", "", "");
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    assertFalse(validator.isValid(command("new@example.com", "other"), context));
  }
}
