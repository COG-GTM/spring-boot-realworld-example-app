package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginServiceTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private LoginService loginService;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = spy(new BCryptPasswordEncoder(4));
    loginService = new LoginService(userRepository, passwordEncoder);
    user = new User("a@b.com", "a", passwordEncoder.encode("secret"), "", "");
    when(userRepository.findByEmail(eq("a@b.com"))).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(eq("nobody@b.com"))).thenReturn(Optional.empty());
  }

  @Test
  public void should_return_user_for_correct_password() {
    assertEquals(Optional.of(user), loginService.authenticate("a@b.com", "secret"));
  }

  @Test
  public void should_reject_wrong_password() {
    assertFalse(loginService.authenticate("a@b.com", "wrong").isPresent());
    verify(passwordEncoder).matches(eq("wrong"), eq(user.getPassword()));
  }

  @Test
  public void should_still_check_a_bcrypt_hash_when_email_is_unknown() {
    assertFalse(loginService.authenticate("nobody@b.com", "secret").isPresent());

    ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
    verify(passwordEncoder, times(1)).matches(eq("secret"), hash.capture());
    assertTrue(hash.getValue().startsWith("$2a$04$"), "dummy hash uses the same encoder cost");
    assertNotEquals(user.getPassword(), hash.getValue());
  }

  @Test
  public void should_handle_null_password_without_skipping_the_hash_check() {
    assertFalse(loginService.authenticate("nobody@b.com", null).isPresent());
    verify(passwordEncoder, times(1)).matches(eq(""), anyString());
  }
}
