package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The REST API reads root-wrapped JSON (spring.jackson.deserialization.UNWRAP_ROOT_VALUE), but
 * GraphQL request bodies are not wrapped, so /graphql gets its own JSON converter. Raw {@code
 * application/graphql} bodies are converted to JSON by {@link GraphQLRawQueryFilter}.
 */
@Configuration
public class GraphQLHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    ObjectMapper graphQlMapper =
        objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(graphQlMapper));
  }

  @Bean
  public FilterRegistrationBean<GraphQLRawQueryFilter> graphQlRawQueryFilter() {
    FilterRegistrationBean<GraphQLRawQueryFilter> registration =
        new FilterRegistrationBean<>(new GraphQLRawQueryFilter());
    registration.addUrlPatterns("/graphql");
    return registration;
  }
}
