package io.spring.infrastructure.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultJwtService implements JwtService {
  private final MacAlgorithm algorithm;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final Duration sessionTime;

  @Autowired
  public DefaultJwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.sessionTime}") int sessionTime) {
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    this.algorithm = macAlgorithmFor(secretBytes);
    SecretKey signingKey = new SecretKeySpec(secretBytes, jcaName(algorithm));
    this.sessionTime = Duration.ofSeconds(sessionTime);
    this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(signingKey).macAlgorithm(algorithm).build();
    decoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
    this.jwtDecoder = decoder;
  }

  @Override
  public String toToken(User user) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getId())
            .expiresAt(Instant.now().plus(sessionTime))
            .build();
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims))
        .getTokenValue();
  }

  @Override
  public Optional<String> getSubFromToken(String token) {
    try {
      return Optional.ofNullable(jwtDecoder.decode(token).getSubject());
    } catch (JwtException e) {
      return Optional.empty();
    }
  }

  /** Strongest HMAC algorithm the secret supports (RFC 7518 §3.2 minimum key sizes). */
  private static MacAlgorithm macAlgorithmFor(byte[] secret) {
    if (secret.length >= 64) {
      return MacAlgorithm.HS512;
    } else if (secret.length >= 48) {
      return MacAlgorithm.HS384;
    } else if (secret.length >= 32) {
      return MacAlgorithm.HS256;
    }
    throw new IllegalArgumentException("jwt.secret must be at least 256 bits (32 bytes)");
  }

  private static String jcaName(MacAlgorithm algorithm) {
    return "Hmac" + algorithm.getName().replace("HS", "SHA");
  }
}
