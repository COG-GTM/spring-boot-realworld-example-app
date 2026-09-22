package io.spring.api.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
  private boolean enabled = true;
  private int anonymousPerMinute = 60;
  private int authenticatedPerMinute = 600;
  private String healthPath = "/actuator/health";
  private boolean trustForwardedHeaders = false;
}
