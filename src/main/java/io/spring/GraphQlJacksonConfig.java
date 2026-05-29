package io.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;

/**
 * Configures a separate ObjectMapper for the GraphQL RouterFunction endpoint. The global Jackson
 * setting UNWRAP_ROOT_VALUE=true (needed by REST controllers using @JsonRootName) is incompatible
 * with Spring GraphQL's request deserialization. This replaces the message converter on
 * RouterFunctionMapping with one that does not unwrap root values.
 */
@Configuration
public class GraphQlJacksonConfig {

  @Bean
  static BeanPostProcessor routerFunctionMappingCustomizer() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {
        if (bean instanceof RouterFunctionMapping mapping) {
          ObjectMapper graphQlMapper = new ObjectMapper();
          mapping.setMessageConverters(
              List.of(new MappingJackson2HttpMessageConverter(graphQlMapper)));
        }
        return bean;
      }
    };
  }
}
