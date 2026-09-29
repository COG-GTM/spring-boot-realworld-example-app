package io.spring.infrastructure.ratelimit;

import io.spring.core.ratelimit.RateLimitAction;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
  private boolean enabled = true;

  /** Per-action limits; actions without an entry are not limited. */
  private Map<RateLimitAction, Limit> limits = new EnumMap<>(RateLimitAction.class);

  @Getter
  @Setter
  public static class Limit {
    private int requests;
    private Duration window = Duration.ofMinutes(1);

    public Limit() {}

    public Limit(int requests, Duration window) {
      this.requests = requests;
      this.window = window;
    }
  }
}
