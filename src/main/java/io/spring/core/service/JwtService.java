package io.spring.core.service;

import io.spring.core.user.User;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public interface JwtService {
  String toToken(User user);

  String generateAccessToken(User user);

  String generateRefreshToken(User user);

  Optional<String> getSubFromToken(String token);

  Optional<String> validateRefreshToken(String token);

  SecretKey getSigningKey();
}
