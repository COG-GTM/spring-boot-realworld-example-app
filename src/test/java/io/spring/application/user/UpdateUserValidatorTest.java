package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateUserValidatorTest {

  @Mock private UserRepository userRepository;

  @Mock private ConstraintValidatorContext context;

  private UpdateUserValidator validator;

  private User targetUser;

  @BeforeEach
  void setUp() {
    validator = new UpdateUserValidator();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
    targetUser = new User("target@example.com", "target", "pass", "", "");
  }

  private UpdateUserCommand commandWith(String email, String username) {
    UpdateUserParam param = UpdateUserParam.builder().email(email).username(username).build();
    return new UpdateUserCommand(targetUser, param);
  }

  private void stubViolationBuilder() {
    ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
    NodeBuilderCustomizableContext node = mock(NodeBuilderCustomizableContext.class);
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    lenient().when(builder.addPropertyNode(anyString())).thenReturn(node);
    lenient().when(node.addConstraintViolation()).thenReturn(context);
  }

  @Test
  void valid_when_email_and_username_are_free() {
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    assertThat(validator.isValid(commandWith("new@example.com", "newname"), context)).isTrue();
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  void valid_when_email_and_username_belong_to_target_user_itself() {
    when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));

    assertThat(validator.isValid(commandWith("target@example.com", "target"), context)).isTrue();
  }

  @Test
  void invalid_when_email_taken_by_another_user() {
    User other = new User("other@example.com", "other", "pass", "", "");
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());
    stubViolationBuilder();

    assertThat(validator.isValid(commandWith("other@example.com", "newname"), context)).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
  }

  @Test
  void invalid_when_username_taken_by_another_user() {
    User other = new User("other@example.com", "other", "pass", "", "");
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
    stubViolationBuilder();

    assertThat(validator.isValid(commandWith("new@example.com", "other"), context)).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  @Test
  void invalid_when_both_email_and_username_taken_by_another_user() {
    User other = new User("other@example.com", "other", "pass", "", "");
    when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
    stubViolationBuilder();

    assertThat(validator.isValid(commandWith("other@example.com", "other"), context)).isFalse();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }
}
