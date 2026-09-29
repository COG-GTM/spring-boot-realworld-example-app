package io.spring.infrastructure.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfiguration {

  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(
      LoggingProperties properties) {
    FilterRegistrationBean<CorrelationIdFilter> registration =
        new FilterRegistrationBean<>(new CorrelationIdFilter(properties.getCorrelation()));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "app.logging.http",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public FilterRegistrationBean<RequestResponseLoggingFilter> requestResponseLoggingFilter(
      LoggingProperties properties) {
    FilterRegistrationBean<RequestResponseLoggingFilter> registration =
        new FilterRegistrationBean<>(new RequestResponseLoggingFilter(properties.getHttp()));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "app.logging.service",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public ServiceLoggingAspect serviceLoggingAspect() {
    return new ServiceLoggingAspect();
  }

  @Bean
  public TaskExecutorCustomizer mdcTaskExecutorCustomizer() {
    return executor -> executor.setTaskDecorator(new MdcTaskDecorator());
  }
}
