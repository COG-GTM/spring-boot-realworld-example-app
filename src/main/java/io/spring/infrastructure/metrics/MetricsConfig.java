package io.spring.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public MyBatisMetricsInterceptor myBatisMetricsInterceptor(MeterRegistry registry) {
    return new MyBatisMetricsInterceptor(registry);
  }
}
