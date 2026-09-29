package io.spring.api.ratelimit;

import io.spring.core.ratelimit.RateLimiter;
import io.spring.infrastructure.ratelimit.FixedWindowRateLimiter;
import io.spring.infrastructure.ratelimit.RateLimitProperties;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig implements WebMvcConfigurer {
  @Autowired private RateLimitProperties properties;

  @Bean
  public RateLimiter rateLimiter() {
    return new FixedWindowRateLimiter(properties, Clock.systemUTC());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RateLimitInterceptor(rateLimiter()));
  }
}
