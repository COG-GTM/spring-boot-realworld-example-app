package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.TestHelper;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/smiley.jpg";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Captor private ArgumentCaptor<User> userCaptor;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
  }

  @Test
  void should_save_user_with_encoded_password_empty_bio_and_default_image() {
    when(passwordEncoder.encode("123")).thenReturn("encoded-123");

    User created = userService.createUser(new RegisterParam("john@test.com", "john", "123"));

    verify(userRepository).save(userCaptor.capture());
    User saved = userCaptor.getValue();
    assertThat(saved).isSameAs(created);
    assertThat(saved.getId()).isNotBlank();
    assertThat(saved.getEmail()).isEqualTo("john@test.com");
    assertThat(saved.getUsername()).isEqualTo("john");
    assertThat(saved.getPassword()).isEqualTo("encoded-123");
    assertThat(saved.getBio()).isEmpty();
    assertThat(saved.getImage()).isEqualTo(DEFAULT_IMAGE);
  }

  @Test
  void should_update_user_and_save_it() {
    User user = TestHelper.userFixture("john");

    userService.updateUser(
        new UpdateUserCommand(
            user,
            UpdateUserParam.builder()
                .email("new@test.com")
                .username("newname")
                .password("new-password")
                .bio("new bio")
                .image("new-image.jpg")
                .build()));

    verify(userRepository).save(user);
    assertThat(user.getEmail()).isEqualTo("new@test.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("new-password");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new-image.jpg");
  }

  @Test
  void should_leave_user_unchanged_when_update_param_fields_are_empty() {
    User user = TestHelper.userFixture("john");

    userService.updateUser(new UpdateUserCommand(user, UpdateUserParam.builder().build()));

    verify(userRepository).save(user);
    assertThat(user.getEmail()).isEqualTo("john@test.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("john bio");
    assertThat(user.getImage()).isEqualTo("https://images.com/john.jpg");
  }

  @Test
  void should_only_update_provided_fields() {
    User user = TestHelper.userFixture("john");

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().bio("updated bio").build()));

    assertThat(user.getBio()).isEqualTo("updated bio");
    assertThat(user.getEmail()).isEqualTo("john@test.com");
    assertThat(user.getUsername()).isEqualTo("john");
  }

  @Nested
  class UpdateUserValidatorTest {

    @Mock private UserRepository validatorUserRepository;
    @Mock private ConstraintValidatorContext context;
    @Mock private ConstraintViolationBuilder violationBuilder;
    @Mock private NodeBuilderCustomizableContext nodeBuilder;

    private UpdateUserValidator validator;
    private User targetUser;

    @BeforeEach
    void setUp() {
      validator = new UpdateUserValidator();
      ReflectionTestUtils.setField(validator, "userRepository", validatorUserRepository);
      targetUser = TestHelper.userFixture("john");
    }

    private void stubViolationBuilder() {
      when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
      when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
      when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    private UpdateUserCommand commandWith(String email, String username) {
      return new UpdateUserCommand(
          targetUser, UpdateUserParam.builder().email(email).username(username).build());
    }

    @Test
    void should_be_valid_when_email_and_username_are_unused() {
      when(validatorUserRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
      when(validatorUserRepository.findByUsername("newname")).thenReturn(Optional.empty());

      assertThat(validator.isValid(commandWith("new@test.com", "newname"), context)).isTrue();
      verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void should_be_valid_when_email_and_username_belong_to_the_target_user() {
      when(validatorUserRepository.findByEmail("john@test.com"))
          .thenReturn(Optional.of(targetUser));
      when(validatorUserRepository.findByUsername("john")).thenReturn(Optional.of(targetUser));

      assertThat(validator.isValid(commandWith("john@test.com", "john"), context)).isTrue();
      verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void should_be_invalid_and_report_email_when_email_belongs_to_another_user() {
      stubViolationBuilder();
      when(validatorUserRepository.findByEmail("taken@test.com"))
          .thenReturn(Optional.of(TestHelper.userFixture("other")));
      when(validatorUserRepository.findByUsername("newname")).thenReturn(Optional.empty());

      assertThat(validator.isValid(commandWith("taken@test.com", "newname"), context)).isFalse();

      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate("email already exist");
      verify(violationBuilder).addPropertyNode("email");
      verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    void should_be_invalid_and_report_username_when_username_belongs_to_another_user() {
      stubViolationBuilder();
      when(validatorUserRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
      when(validatorUserRepository.findByUsername("taken"))
          .thenReturn(Optional.of(TestHelper.userFixture("other")));

      assertThat(validator.isValid(commandWith("new@test.com", "taken"), context)).isFalse();

      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate("username already exist");
      verify(violationBuilder).addPropertyNode("username");
      verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    void should_report_both_violations_when_email_and_username_are_taken() {
      stubViolationBuilder();
      User other = TestHelper.userFixture("other");
      when(validatorUserRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(other));
      when(validatorUserRepository.findByUsername("taken")).thenReturn(Optional.of(other));

      assertThat(validator.isValid(commandWith("taken@test.com", "taken"), context)).isFalse();

      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate(eq("email already exist"));
      verify(context).buildConstraintViolationWithTemplate(eq("username already exist"));
      verify(violationBuilder).addPropertyNode("email");
      verify(violationBuilder).addPropertyNode("username");
      verify(nodeBuilder, times(2)).addConstraintViolation();
    }
  }
}
