package io.spring.api.ratelimit;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

  @Bean
  public RateLimiter rateLimiter() {
    return new RateLimiter(Clock.systemUTC());
  }
}
