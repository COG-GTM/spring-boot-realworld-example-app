package io.spring.api.ratelimit;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
  private boolean enabled = true;
  private Limit auth = new Limit(10, Duration.ofMinutes(1));
  private Limit articleCreation = new Limit(30, Duration.ofHours(1));

  public Limit forPolicy(RateLimitPolicy policy) {
    switch (policy) {
      case AUTH:
        return auth;
      case ARTICLE_CREATION:
        return articleCreation;
      default:
        throw new IllegalArgumentException("Unknown rate limit policy " + policy);
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Limit {
    private int limit;
    private Duration window;
  }
}
