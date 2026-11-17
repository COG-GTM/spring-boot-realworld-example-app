package io.spring.api;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Spring MVC 6 no longer maps {@code /articles/} to {@code /articles}. This filter strips the
 * trailing slash before the request reaches MVC, so those URLs keep working. It runs after the
 * Spring Security filter chain, which still authorizes the path as sent.
 */
@Configuration
public class TrailingSlashConfig {

  @Bean
  public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
    FilterRegistrationBean<UrlHandlerFilter> registration =
        new FilterRegistrationBean<>(
            UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
    registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
    return registration;
  }
}
