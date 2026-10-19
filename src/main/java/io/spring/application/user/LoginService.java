package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Checks email/password credentials. The password is always run through the encoder, against a
 * dummy hash when the email is unknown, so the response time does not reveal whether an account
 * exists.
 */
@Service
public class LoginService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String dummyPasswordHash;

  public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public Optional<User> authenticate(String email, String password) {
    Optional<User> user = userRepository.findByEmail(email);
    String passwordHash = user.map(User::getPassword).orElse(dummyPasswordHash);
    boolean matches = passwordEncoder.matches(password == null ? "" : password, passwordHash);
    return matches ? user : Optional.empty();
  }
}
