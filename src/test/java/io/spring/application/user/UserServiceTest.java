package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

public class UserServiceTest {

  private static final String DEFAULT_IMAGE = "https://static.productionready.io/images/user.jpg";

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private AnnotationConfigApplicationContext context;
  private UserService userService;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);

    // UserService is @Validated: RegisterParam and UpdateUserCommand are only validated through
    // the Spring proxy, and the constraint validators get their UserRepository injected by the
    // Spring backed constraint validator factory.
    context = new AnnotationConfigApplicationContext();
    context.registerBean(UserRepository.class, () -> userRepository);
    context.registerBean(PasswordEncoder.class, () -> passwordEncoder);
    context.registerBean(LocalValidatorFactoryBean.class, LocalValidatorFactoryBean::new);
    context.registerBean(
        MethodValidationPostProcessor.class,
        () -> {
          MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
          processor.setValidator(context.getBean(LocalValidatorFactoryBean.class));
          return processor;
        });
    context.registerBean(
        UserService.class, () -> new UserService(userRepository, DEFAULT_IMAGE, passwordEncoder));
    context.refresh();

    userService = context.getBean(UserService.class);
  }

  @AfterEach
  public void tearDown() {
    context.close();
  }

  @Test
  public void should_create_user_with_encoded_password_and_default_image() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");

    User created = userService.createUser(new RegisterParam("john@example.com", "john", "123456"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertThat(saved).isSameAs(created);
    assertThat(saved.getEmail()).isEqualTo("john@example.com");
    assertThat(saved.getUsername()).isEqualTo("john");
    assertThat(saved.getPassword()).isEqualTo("encoded-123456");
    assertThat(saved.getBio()).isEmpty();
    assertThat(saved.getImage()).isEqualTo(DEFAULT_IMAGE);
    assertThat(saved.getId()).isNotBlank();
  }

  @Test
  public void should_reject_registration_with_an_already_used_email() {
    when(userRepository.findByEmail("john@example.com"))
        .thenReturn(Optional.of(new User("john@example.com", "existing", "123", "", "")));
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () -> userService.createUser(new RegisterParam("john@example.com", "john", "123456")))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("duplicated email"));

    verify(userRepository, never()).save(any());
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  public void should_reject_registration_with_an_already_used_username() {
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("john"))
        .thenReturn(Optional.of(new User("other@example.com", "john", "123", "", "")));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () -> userService.createUser(new RegisterParam("john@example.com", "john", "123456")))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("duplicated username"));

    verify(userRepository, never()).save(any());
  }

  @Test
  public void should_update_all_the_fields_of_the_target_user() {
    User user = new User("john@example.com", "john", "123", "old bio", "old image");
    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    userService.updateUser(
        new UpdateUserCommand(
            user,
            UpdateUserParam.builder()
                .email("new@example.com")
                .username("newname")
                .password("new password")
                .bio("new bio")
                .image("new image")
                .build()));

    verify(userRepository).save(user);
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("new password");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_keep_the_fields_left_empty_in_the_update_param() {
    User user = new User("john@example.com", "john", "123", "old bio", "old image");
    when(userRepository.findByEmail("")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("")).thenReturn(Optional.empty());

    userService.updateUser(
        new UpdateUserCommand(user, UpdateUserParam.builder().bio("new bio").build()));

    verify(userRepository).save(user);
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_accept_an_update_keeping_the_email_and_username_of_the_same_user() {
    User user = new User("john@example.com", "john", "123", "", "");
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

    userService.updateUser(
        new UpdateUserCommand(
            user,
            UpdateUserParam.builder()
                .email("john@example.com")
                .username("john")
                .bio("new bio")
                .build()));

    verify(userRepository).save(user);
    assertThat(user.getBio()).isEqualTo("new bio");
  }

  @Test
  public void should_reject_an_update_with_the_email_of_another_user() {
    User user = new User("john@example.com", "john", "123", "", "");
    User anotherUser = new User("taken@example.com", "other", "123", "", "");
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(anotherUser));
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.updateUser(
                    new UpdateUserCommand(
                        user,
                        UpdateUserParam.builder()
                            .email("taken@example.com")
                            .username("john")
                            .build())))
        .satisfies(
            exception -> {
              assertThat(exception.getConstraintViolations())
                  .extracting(ConstraintViolation::getMessage)
                  .containsExactly("email already exist");
              assertThat(exception.getConstraintViolations())
                  .allSatisfy(
                      violation ->
                          assertThat(violation.getPropertyPath().toString()).endsWith("email"));
            });

    verify(userRepository, never()).save(any());
    assertThat(user.getEmail()).isEqualTo("john@example.com");
  }

  @Test
  public void should_reject_an_update_with_the_username_of_another_user() {
    User user = new User("john@example.com", "john", "123", "", "");
    User anotherUser = new User("other@example.com", "taken", "123", "", "");
    when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(anotherUser));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.updateUser(
                    new UpdateUserCommand(
                        user,
                        UpdateUserParam.builder()
                            .email("john@example.com")
                            .username("taken")
                            .build())))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("username already exist"));

    verify(userRepository, never()).save(any());
  }

  @Test
  public void should_report_both_email_and_username_when_both_are_taken() {
    User user = new User("john@example.com", "john", "123", "", "");
    when(userRepository.findByEmail("taken@example.com"))
        .thenReturn(Optional.of(new User("taken@example.com", "someone", "123", "", "")));
    when(userRepository.findByUsername("taken"))
        .thenReturn(Optional.of(new User("other@example.com", "taken", "123", "", "")));

    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(
            () ->
                userService.updateUser(
                    new UpdateUserCommand(
                        user,
                        UpdateUserParam.builder()
                            .email("taken@example.com")
                            .username("taken")
                            .build())))
        .satisfies(
            exception ->
                assertThat(exception.getConstraintViolations())
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder("email already exist", "username already exist"));

    verify(userRepository, never()).save(any());
  }
}
