package io.spring.api.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
  private int unauthenticatedPerMinute = 60;
  private int authenticatedPerMinute = 600;
}
