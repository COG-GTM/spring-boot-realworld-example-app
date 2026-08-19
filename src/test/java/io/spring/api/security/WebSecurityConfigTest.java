package io.spring.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

public class WebSecurityConfigTest {

  private PasswordEncoder passwordEncoderWithStrength(int strength) {
    WebSecurityConfig config = new WebSecurityConfig();
    ReflectionTestUtils.setField(config, "bcryptStrength", strength);
    return config.passwordEncoder();
  }

  @Test
  public void should_use_bcrypt_with_at_least_strength_12() {
    String hash = passwordEncoderWithStrength(12).encode("johnnyjacob");

    assertThat(hash).startsWith("$2a$12$");
  }

  @Test
  public void should_match_password_hashed_by_the_encoder() {
    PasswordEncoder encoder = passwordEncoderWithStrength(12);
    String hash = encoder.encode("johnnyjacob");

    assertThat(encoder.matches("johnnyjacob", hash)).isTrue();
    assertThat(encoder.matches("wrong-password", hash)).isFalse();
  }

  @Test
  public void should_reject_configured_strength_below_minimum() {
    assertThatThrownBy(() -> passwordEncoderWithStrength(10))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 12");
  }
}
