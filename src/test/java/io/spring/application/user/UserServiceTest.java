package io.spring.application.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Focused test for {@link UserService}. A minimal Spring context wires the service behind a {@link
 * MethodValidationPostProcessor} so the {@code @Validated} method-level validation fires, exercising
 * the duplicate email/username constraints (whose validators autowire the mocked {@link
 * UserRepository}).
 */
@SpringBootTest(
    classes = {UserService.class, UserServiceTest.ValidationConfig.class},
    properties = "image.default=https://static.productionready.io/images/smiley-cyrus.jpg")
public class UserServiceTest {

  @Configuration
  static class ValidationConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor(
        LocalValidatorFactoryBean validator) {
      MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
      processor.setValidator(validator);
      return processor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
      PasswordEncoder encoder = org.mockito.Mockito.mock(PasswordEncoder.class);
      when(encoder.encode(any())).thenReturn("encoded");
      return encoder;
    }
  }

  @Autowired private UserService userService;

  @MockBean private UserRepository userRepository;

  private User existingUser;

  @BeforeEach
  public void setUp() {
    existingUser = new User("existing@test.com", "existing", "123", "", "");
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
  }

  @Test
  public void should_create_user() {
    RegisterParam param = new RegisterParam("new@test.com", "newuser", "password");

    User user = userService.createUser(param);

    Assertions.assertEquals("new@test.com", user.getEmail());
    Assertions.assertEquals("newuser", user.getUsername());
    Assertions.assertEquals("encoded", user.getPassword());
    verify(userRepository).save(any(User.class));
  }

  @Test
  public void should_reject_create_user_with_blank_fields() {
    RegisterParam param = new RegisterParam("", "", "");

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_reject_create_user_with_invalid_email_format() {
    RegisterParam param = new RegisterParam("not-an-email", "newuser", "password");

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_reject_create_user_with_duplicated_email() {
    when(userRepository.findByEmail(eq("dup@test.com"))).thenReturn(Optional.of(existingUser));
    RegisterParam param = new RegisterParam("dup@test.com", "newuser", "password");

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_reject_create_user_with_duplicated_username() {
    when(userRepository.findByUsername(eq("existing"))).thenReturn(Optional.of(existingUser));
    RegisterParam param = new RegisterParam("new@test.com", "existing", "password");

    Assertions.assertThrows(
        ConstraintViolationException.class, () -> userService.createUser(param));
  }

  @Test
  public void should_update_user() {
    User target = new User("target@test.com", "target", "123", "old bio", "old image");
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("updated@test.com")
            .username("updated")
            .bio("new bio")
            .image("new image")
            .password("")
            .build();

    userService.updateUser(new UpdateUserCommand(target, param));

    Assertions.assertEquals("updated@test.com", target.getEmail());
    Assertions.assertEquals("updated", target.getUsername());
    Assertions.assertEquals("new bio", target.getBio());
    verify(userRepository).save(target);
  }

  @Test
  public void should_reject_update_user_with_email_used_by_another_user() {
    User target = new User("target@test.com", "target", "123", "", "");
    User another = new User("taken@test.com", "another", "123", "", "");
    when(userRepository.findByEmail(eq("taken@test.com"))).thenReturn(Optional.of(another));
    UpdateUserParam param = UpdateUserParam.builder().email("taken@test.com").build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> userService.updateUser(new UpdateUserCommand(target, param)));
  }

  @Test
  public void should_reject_update_user_with_username_used_by_another_user() {
    User target = new User("target@test.com", "target", "123", "", "");
    User another = new User("another@test.com", "taken", "123", "", "");
    when(userRepository.findByUsername(eq("taken"))).thenReturn(Optional.of(another));
    UpdateUserParam param = UpdateUserParam.builder().username("taken").build();

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> userService.updateUser(new UpdateUserCommand(target, param)));
  }
}
