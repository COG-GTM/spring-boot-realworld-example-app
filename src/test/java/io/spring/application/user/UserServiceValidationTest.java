package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = {UserService.class, ValidationAutoConfiguration.class},
    properties = "image.default=https://example.com/default.png")
public class UserServiceValidationTest {
  @Autowired private UserService userService;

  @MockBean private UserRepository userRepository;

  @MockBean private PasswordEncoder passwordEncoder;

  private User existing;

  @BeforeEach
  public void setUp() {
    existing = new User("taken@example.com", "taken", "123", "", "");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
  }

  @Test
  public void should_reject_blank_and_malformed_registration() {
    ConstraintViolationException e =
        assertThrows(
            ConstraintViolationException.class,
            () -> userService.createUser(new RegisterParam("not-an-email", "", "")));

    Map<String, String> messages = messagesByLeafProperty(e);
    assertEquals("should be an email", messages.get("email"));
    assertEquals("can't be empty", messages.get("username"));
    assertEquals("can't be empty", messages.get("password"));
    verify(userRepository, never()).save(any());
  }

  @Test
  public void should_reject_duplicated_email_and_username_on_registration() {
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existing));

    ConstraintViolationException e =
        assertThrows(
            ConstraintViolationException.class,
            () -> userService.createUser(new RegisterParam("taken@example.com", "taken", "pw")));

    Map<String, String> messages = messagesByLeafProperty(e);
    assertEquals("duplicated email", messages.get("email"));
    assertEquals("duplicated username", messages.get("username"));
    verify(userRepository, never()).save(any());
  }

  @Test
  public void should_register_valid_user() {
    when(passwordEncoder.encode("pw")).thenReturn("encoded");

    User user = userService.createUser(new RegisterParam("new@example.com", "newbie", "pw"));

    assertEquals("encoded", user.getPassword());
    assertEquals("https://example.com/default.png", user.getImage());
    verify(userRepository).save(user);
  }

  @Test
  public void should_reject_update_to_email_and_username_owned_by_another_user() {
    User target = new User("me@example.com", "me", "123", "", "");
    when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));
    when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existing));
    UpdateUserParam param =
        UpdateUserParam.builder().email("taken@example.com").username("taken").build();

    ConstraintViolationException e =
        assertThrows(
            ConstraintViolationException.class,
            () -> userService.updateUser(new UpdateUserCommand(target, param)));

    Map<String, String> messages = messagesByLeafProperty(e);
    assertEquals("email already exist", messages.get("email"));
    assertEquals("username already exist", messages.get("username"));
    assertEquals("me@example.com", target.getEmail());
    verify(userRepository, never()).save(any());
  }

  @Test
  public void malformed_email_on_update_is_not_validated_by_service() {
    // UpdateUserCommand does not cascade @Valid to UpdateUserParam, so @Email is only enforced
    // where callers validate UpdateUserParam themselves (e.g. CurrentUserApi).
    User target = new User("me@example.com", "me", "123", "", "");
    UpdateUserParam param = UpdateUserParam.builder().email("not-an-email").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("not-an-email", target.getEmail());
    verify(userRepository).save(target);
  }

  @Test
  public void should_allow_update_keeping_own_email_and_username() {
    User target = new User("me@example.com", "me", "123", "", "");
    when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(target));
    when(userRepository.findByUsername("me")).thenReturn(Optional.of(target));
    UpdateUserParam param =
        UpdateUserParam.builder().email("me@example.com").username("me").bio("new bio").build();

    userService.updateUser(new UpdateUserCommand(target, param));

    assertEquals("new bio", target.getBio());
    verify(userRepository).save(target);
  }

  private static Map<String, String> messagesByLeafProperty(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .collect(
            Collectors.toMap(
                UserServiceValidationTest::leaf,
                ConstraintViolation::getMessage,
                (a, b) -> a + "," + b));
  }

  private static String leaf(ConstraintViolation<?> v) {
    String path = v.getPropertyPath().toString();
    return path.substring(path.lastIndexOf('.') + 1);
  }
}
