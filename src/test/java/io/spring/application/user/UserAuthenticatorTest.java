package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

public class UserAuthenticatorTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserAuthenticator authenticator;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = spy(new BCryptPasswordEncoder());
    user = new User("a@b.com", "a", passwordEncoder.encode("secret"), "", "");
    authenticator = new UserAuthenticator(userRepository, passwordEncoder);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(eq("a@b.com"))).thenReturn(Optional.of(user));
  }

  @Test
  public void should_return_user_for_valid_credentials() {
    assertEquals(Optional.of(user), authenticator.authenticate("a@b.com", "secret"));
  }

  @Test
  public void should_reject_wrong_password() {
    assertFalse(authenticator.authenticate("a@b.com", "wrong").isPresent());
    verify(passwordEncoder).matches(eq("wrong"), eq(user.getPassword()));
  }

  @Test
  public void should_run_bcrypt_against_dummy_hash_for_unknown_email() {
    assertFalse(authenticator.authenticate("nobody@b.com", "secret").isPresent());

    ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
    verify(passwordEncoder).matches(eq("secret"), hash.capture());
    assertTrue(hash.getValue().startsWith("$2a$10$"));
    assertFalse(hash.getValue().equals(user.getPassword()));
  }

  @Test
  public void should_reject_null_password_without_calling_encoder() {
    assertFalse(authenticator.authenticate("a@b.com", null).isPresent());
    verify(passwordEncoder, never()).matches(eq(null), anyString());
  }
}
