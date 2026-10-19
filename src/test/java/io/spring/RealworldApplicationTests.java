package io.spring;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public class RealworldApplicationTests {

  @DynamicPropertySource
  static void jwtSecret(DynamicPropertyRegistry registry) {
    byte[] secret = new byte[64];
    new SecureRandom().nextBytes(secret);
    registry.add("jwt.secret", () -> Base64.getEncoder().encodeToString(secret));
  }

  @Test
  public void contextLoads() {}
}
