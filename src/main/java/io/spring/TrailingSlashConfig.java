package io.spring;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class TrailingSlashConfig {

  // Spring 6+ no longer matches "/path/" to "/path"; strip the slash before security and MVC.
  @Bean
  public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
    FilterRegistrationBean<UrlHandlerFilter> registration =
        new FilterRegistrationBean<>(
            UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
