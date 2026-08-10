package io.spring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UpdateUserParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.lang.reflect.Constructor;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final UserService userService =
      new UserService(userRepository, "default-image", passwordEncoder);

  @Test
  void should_create_user_with_encoded_password_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");
    RegisterParam registerParam = new RegisterParam("jake@jake.jake", "jake", "123");

    User user = userService.createUser(registerParam);

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("encoded-123");
    assertThat(user.getBio()).isEmpty();
    assertThat(user.getImage()).isEqualTo("default-image");
    verify(userRepository).save(user);
  }

  @Test
  void should_update_user_with_provided_values() {
    User user = new User("jake@jake.jake", "jake", "123", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("new@email.com")
            .username("newname")
            .password("newpassword")
            .bio("new bio")
            .image("new image")
            .build();

    userService.updateUser(new UpdateUserCommand(user, param));

    assertThat(user.getEmail()).isEqualTo("new@email.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpassword");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new image");
    verify(userRepository).save(user);
  }

  @Test
  void should_keep_original_values_when_update_param_is_empty() {
    User user = new User("jake@jake.jake", "jake", "123", "old bio", "old image");

    userService.updateUser(new UpdateUserCommand(user, UpdateUserParam.builder().build()));

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("old bio");
    assertThat(user.getImage()).isEqualTo("old image");
    verify(userRepository).save(user);
  }

  @Test
  void update_user_validator_should_accept_values_not_used_by_another_user() throws Exception {
    User target = new User("jake@jake.jake", "jake", "123", "bio", "image");
    when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.of(target));
    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    boolean valid =
        validateUpdateUserCommand(
            new UpdateUserCommand(
                target,
                UpdateUserParam.builder().email("new@email.com").username("newname").build()),
            context);

    assertThat(valid).isTrue();
    verify(context, never()).disableDefaultConstraintViolation();
  }

  @Test
  void update_user_validator_should_reject_email_and_username_owned_by_another_user()
      throws Exception {
    User target = new User("jake@jake.jake", "jake", "123", "bio", "image");
    User other = new User("other@email.com", "other", "123", "bio", "image");
    when(userRepository.findByEmail("other@email.com")).thenReturn(Optional.of(other));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

    ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
    ConstraintViolationBuilder violationBuilder = mock(ConstraintViolationBuilder.class);
    NodeBuilderCustomizableContext nodeBuilder = mock(NodeBuilderCustomizableContext.class);
    when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode(any())).thenReturn(nodeBuilder);

    boolean valid =
        validateUpdateUserCommand(
            new UpdateUserCommand(
                target,
                UpdateUserParam.builder().email("other@email.com").username("other").build()),
            context);

    assertThat(valid).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
    verify(violationBuilder).addPropertyNode("email");
    verify(violationBuilder).addPropertyNode("username");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean validateUpdateUserCommand(
      UpdateUserCommand command, ConstraintValidatorContext context) throws Exception {
    Class<?> validatorClass = Class.forName("io.spring.application.user.UpdateUserValidator");
    Constructor<?> constructor = validatorClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    ConstraintValidator validator = (ConstraintValidator) constructor.newInstance();
    ReflectionTestUtils.setField(validator, "userRepository", userRepository);
    return validator.isValid(command, context);
  }
}
