package io.spring.api.ratelimit;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig implements WebMvcConfigurer {
  private final RateLimitProperties properties;

  public RateLimitConfig(RateLimitProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (!properties.isEnabled()) {
      return;
    }
    Map<RateLimitPolicy, FixedWindowRateLimiter> limiters = new EnumMap<>(RateLimitPolicy.class);
    for (RateLimitPolicy policy : RateLimitPolicy.values()) {
      RateLimitProperties.Limit limit = properties.forPolicy(policy);
      limiters.put(
          policy,
          new FixedWindowRateLimiter(limit.getLimit(), limit.getWindow(), Clock.systemUTC()));
    }
    registry.addInterceptor(new RateLimitInterceptor(limiters));
  }
}
