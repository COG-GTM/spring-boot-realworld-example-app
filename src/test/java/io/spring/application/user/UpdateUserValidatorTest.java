package io.spring.application.user;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateUserValidatorTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private UpdateUserValidator updateUserValidator;

  private User targetUser;
  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    targetUser = new User("old@test.com", "old", "123", "bio", "image");
    context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void should_be_valid_when_email_and_username_are_not_taken() {
    when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

    Assertions.assertTrue(updateUserValidator.isValid(command("new@test.com", "new"), context));
  }

  @Test
  public void should_be_valid_when_email_and_username_belong_to_the_same_user() {
    when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("old")).thenReturn(Optional.of(targetUser));

    Assertions.assertTrue(updateUserValidator.isValid(command("old@test.com", "old"), context));
  }

  @Test
  public void should_be_invalid_when_email_is_taken_by_another_user() {
    User anotherUser = new User("taken@test.com", "another", "123", "", "");
    when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(anotherUser));
    when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

    Assertions.assertFalse(updateUserValidator.isValid(command("taken@test.com", "new"), context));

    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
  }

  @Test
  public void should_be_invalid_when_username_is_taken_by_another_user() {
    User anotherUser = new User("another@test.com", "taken", "123", "", "");
    when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(anotherUser));

    Assertions.assertFalse(updateUserValidator.isValid(command("new@test.com", "taken"), context));

    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  @Test
  public void should_be_invalid_when_both_email_and_username_are_taken() {
    User anotherUser = new User("taken@test.com", "taken", "123", "", "");
    when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(anotherUser));
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(anotherUser));

    Assertions.assertFalse(
        updateUserValidator.isValid(command("taken@test.com", "taken"), context));

    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  private UpdateUserCommand command(String email, String username) {
    return new UpdateUserCommand(
        targetUser, UpdateUserParam.builder().email(email).username(username).build());
  }
}
