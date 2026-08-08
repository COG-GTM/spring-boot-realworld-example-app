package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateUserValidatorTest {

  @Mock private UserRepository userRepository;
  @Mock private ConstraintValidatorContext context;
  @Mock private ConstraintViolationBuilder violationBuilder;
  @Mock private NodeBuilderCustomizableContext nodeBuilder;

  private UpdateUserValidator validator;
  private User targetUser;

  @BeforeEach
  public void setUp() {
    validator = new UpdateUserValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
    targetUser = new User("target@example.com", "target", "123", "bio", "image");
  }

  private void stubViolationBuilding() {
    when(context.buildConstraintViolationWithTemplate(ArgumentMatchers.anyString()))
        .thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode(ArgumentMatchers.anyString())).thenReturn(nodeBuilder);
    when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  private UpdateUserCommand command(String email, String username) {
    return new UpdateUserCommand(
        targetUser, UpdateUserParam.builder().email(email).username(username).build());
  }

  @Test
  public void should_be_valid_when_nothing_is_taken() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

    assertThat(validator.isValid(command("new@example.com", "new"), context)).isTrue();
    verifyNoInteractions(violationBuilder);
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  public void should_be_valid_when_email_and_username_belong_to_the_target_user() {
    when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));

    assertThat(validator.isValid(command("target@example.com", "target"), context)).isTrue();
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  public void should_be_invalid_when_email_belongs_to_another_user() {
    User other = new User("other@example.com", "other", "123", "", "");
    stubViolationBuilding();
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

    assertThat(validator.isValid(command("other@example.com", "new"), context)).isFalse();

    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(violationBuilder).addPropertyNode("email");
    verify(violationBuilder, never()).addPropertyNode("username");
    verify(nodeBuilder).addConstraintViolation();
  }

  @Test
  public void should_be_invalid_when_username_belongs_to_another_user() {
    User other = new User("other@example.com", "other", "123", "", "");
    stubViolationBuilding();
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    assertThat(validator.isValid(command("new@example.com", "other"), context)).isFalse();

    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("username already exist");
    verify(violationBuilder).addPropertyNode("username");
    verify(violationBuilder, never()).addPropertyNode("email");
  }

  @Test
  public void should_report_both_violations_when_email_and_username_are_taken() {
    User other = new User("other@example.com", "other", "123", "", "");
    stubViolationBuilding();
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    assertThat(validator.isValid(command("other@example.com", "other"), context)).isFalse();

    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
    verify(violationBuilder).addPropertyNode("email");
    verify(violationBuilder).addPropertyNode("username");
  }

  @Test
  public void should_be_valid_when_no_change_is_requested() {
    when(userRepository.findByEmail("")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("")).thenReturn(Optional.empty());

    UpdateUserCommand command =
        new UpdateUserCommand(targetUser, UpdateUserParam.builder().build());

    assertThat(validator.isValid(command, context)).isTrue();
    verify(context, never()).disableDefaultConstraintViolation();
  }
}
