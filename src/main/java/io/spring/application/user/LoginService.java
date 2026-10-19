package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LoginService {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private LoginAttemptLimiter loginAttemptLimiter;

  /**
   * Checks the credentials, subject to {@link LoginAttemptLimiter}.
   *
   * @return the user if the credentials match, empty otherwise
   * @throws TooManyLoginAttemptsException if the account or client is throttled
   */
  public Optional<User> authenticate(String email, String password, String clientAddress) {
    LoginAttemptLimiter.Attempt attempt = loginAttemptLimiter.acquire(email, clientAddress);
    Optional<User> user;
    try {
      user =
          userRepository
              .findByEmail(email)
              .filter(u -> passwordEncoder.matches(password, u.getPassword()));
    } catch (RuntimeException e) {
      loginAttemptLimiter.aborted(attempt);
      throw e;
    }
    if (user.isPresent()) {
      loginAttemptLimiter.succeeded(attempt);
    } else {
      loginAttemptLimiter.failed(attempt);
    }
    return user;
  }
}
