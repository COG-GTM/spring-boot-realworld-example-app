package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Drives {@link UserService} through a real bean validation setup so the constraint annotations on
 * {@link RegisterParam} and {@link UpdateUserCommand} are actually exercised.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserServiceValidationTest.TestConfiguration.class)
public class UserServiceValidationTest {

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/smiley.jpg";

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  public void setUp() {
    reset(userRepository);
  }

  @Test
  public void should_create_user_when_register_param_is_valid() {
    when(userRepository.findByEmail("aisensiy@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("aisensiy")).thenReturn(Optional.empty());

    User user = userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "123"));

    assertThat(user.getImage()).isEqualTo(DEFAULT_IMAGE);
    verify(userRepository).save(user);
  }

  @Test
  public void should_reject_register_param_with_invalid_email() {
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () -> userService.createUser(new RegisterParam("not-an-email", "aisensiy", "123")))
        .satisfies(e -> assertThat(messagesOf(e)).contains("should be an email"));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  public void should_reject_register_param_with_duplicated_email() {
    when(userRepository.findByEmail("aisensiy@gmail.com"))
        .thenReturn(Optional.of(new User("aisensiy@gmail.com", "someone", "123", "", "")));
    when(userRepository.findByUsername("aisensiy")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "123")))
        .satisfies(e -> assertThat(messagesOf(e)).contains("duplicated email"));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  public void should_reject_register_param_with_duplicated_username() {
    when(userRepository.findByEmail("aisensiy@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("aisensiy"))
        .thenReturn(Optional.of(new User("someone@gmail.com", "aisensiy", "123", "", "")));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "123")))
        .satisfies(e -> assertThat(messagesOf(e)).contains("duplicated username"));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  public void should_reject_register_param_with_empty_password() {
    when(userRepository.findByEmail("aisensiy@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("aisensiy")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () -> userService.createUser(new RegisterParam("aisensiy@gmail.com", "aisensiy", "")))
        .satisfies(e -> assertThat(messagesOf(e)).contains("can't be empty"));
  }

  @Test
  public void should_update_user_when_new_email_and_username_are_free() {
    User targetUser = new User("old@email.com", "oldname", "123", "", "");
    when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    userService.updateUser(
        new UpdateUserCommand(
            targetUser,
            UpdateUserParam.builder().email("new@email.com").username("newname").build()));

    assertThat(targetUser.getEmail()).isEqualTo("new@email.com");
    assertThat(targetUser.getUsername()).isEqualTo("newname");
    verify(userRepository).save(targetUser);
  }

  @Test
  public void should_allow_update_with_the_users_own_email_and_username() {
    User targetUser = new User("old@email.com", "oldname", "123", "", "");
    when(userRepository.findByEmail("old@email.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("oldname")).thenReturn(Optional.of(targetUser));

    userService.updateUser(
        new UpdateUserCommand(
            targetUser,
            UpdateUserParam.builder()
                .email("old@email.com")
                .username("oldname")
                .bio("new bio")
                .build()));

    assertThat(targetUser.getBio()).isEqualTo("new bio");
    verify(userRepository).save(targetUser);
  }

  @Test
  public void should_reject_update_when_email_belongs_to_another_user() {
    User targetUser = new User("old@email.com", "oldname", "123", "", "");
    when(userRepository.findByEmail("taken@email.com"))
        .thenReturn(Optional.of(new User("taken@email.com", "someone", "123", "", "")));
    when(userRepository.findByUsername("oldname")).thenReturn(Optional.of(targetUser));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.updateUser(
                    new UpdateUserCommand(
                        targetUser,
                        UpdateUserParam.builder()
                            .email("taken@email.com")
                            .username("oldname")
                            .build())))
        .satisfies(e -> assertThat(messagesOf(e)).contains("email already exist"));

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  public void should_reject_update_when_username_belongs_to_another_user() {
    User targetUser = new User("old@email.com", "oldname", "123", "", "");
    when(userRepository.findByEmail("old@email.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("taken"))
        .thenReturn(Optional.of(new User("someone@email.com", "taken", "123", "", "")));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.updateUser(
                    new UpdateUserCommand(
                        targetUser,
                        UpdateUserParam.builder()
                            .email("old@email.com")
                            .username("taken")
                            .build())))
        .satisfies(e -> assertThat(messagesOf(e)).contains("username already exist"));

    verify(userRepository, never()).save(any(User.class));
  }

  private Set<String> messagesOf(ConstraintViolationException exception) {
    return exception.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    public UserRepository userRepository() {
      return mock(UserRepository.class);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
      return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
      return new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder);
    }

    @Bean
    public static LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(
        LocalValidatorFactoryBean validator) {
      MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
      processor.setValidator(validator);
      return processor;
    }
  }
}
