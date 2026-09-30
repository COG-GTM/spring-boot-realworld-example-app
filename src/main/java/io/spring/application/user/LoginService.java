package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final LoginAttemptLimiter loginAttemptLimiter;
  // Hash of a random secret, matched against when the email is unknown so that every login attempt
  // pays the same password-hashing cost.
  private final String dummyPasswordHash;

  public LoginService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      LoginAttemptLimiter loginAttemptLimiter) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.loginAttemptLimiter = loginAttemptLimiter;
    this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  /**
   * Returns the user when the credentials are valid.
   *
   * @throws TooManyLoginAttemptsException when the account or client is currently throttled
   */
  public Optional<User> authenticate(String email, String password, String client) {
    LoginAttemptLimiter.Attempt attempt = loginAttemptLimiter.tryAcquire(email, client);
    if (!attempt.isAllowed()) {
      throw new TooManyLoginAttemptsException(attempt.getRetryAfterSeconds());
    }

    Optional<User> user;
    boolean matches;
    try {
      user = email == null ? Optional.empty() : userRepository.findByEmail(email);
      String hash = user.map(User::getPassword).orElse(dummyPasswordHash);
      matches = passwordEncoder.matches(password == null ? "" : password, hash);
    } catch (RuntimeException e) {
      loginAttemptLimiter.release(attempt);
      throw e;
    }

    if (user.isPresent() && matches) {
      loginAttemptLimiter.recordSuccess(attempt);
      return user;
    }
    return Optional.empty();
  }
}
