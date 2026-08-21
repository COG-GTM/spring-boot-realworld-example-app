package io.spring.application.user;

import io.spring.api.exception.InvalidPasswordResetTokenException;
import io.spring.core.user.PasswordResetToken;
import io.spring.core.user.PasswordResetTokenRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PasswordResetService {
  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final int tokenValiditySeconds;

  @Autowired
  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordEncoder passwordEncoder,
      @Value("${passwordReset.tokenValiditySeconds:3600}") int tokenValiditySeconds) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenValiditySeconds = tokenValiditySeconds;
  }

  public Optional<PasswordResetToken> requestReset(String email) {
    return userRepository
        .findByEmail(email)
        .map(
            user -> {
              PasswordResetToken token =
                  new PasswordResetToken(
                      user.getId(), new DateTime().plusSeconds(tokenValiditySeconds));
              passwordResetTokenRepository.save(token);
              log.info("generated password reset token for user {}", user.getId());
              return token;
            });
  }

  public void resetPassword(String token, String newPassword) {
    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findByToken(token)
            .filter(candidate -> candidate.isValidAt(new DateTime()))
            .orElseThrow(InvalidPasswordResetTokenException::new);

    User user =
        userRepository
            .findById(resetToken.getUserId())
            .orElseThrow(InvalidPasswordResetTokenException::new);

    user.update("", "", passwordEncoder.encode(newPassword), "", "");
    userRepository.save(user);

    resetToken.markUsed();
    passwordResetTokenRepository.save(resetToken);
  }
}
