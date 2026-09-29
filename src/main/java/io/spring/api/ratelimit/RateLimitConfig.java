package io.spring.api.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig implements WebMvcConfigurer {
  private final RateLimitService rateLimitService;

  public RateLimitConfig(RateLimitProperties properties) {
    this.rateLimitService = new RateLimitService(properties, System::nanoTime);
  }

  @Bean
  public RateLimitService rateLimitService() {
    return rateLimitService;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RateLimitInterceptor(rateLimitService));
  }
}
