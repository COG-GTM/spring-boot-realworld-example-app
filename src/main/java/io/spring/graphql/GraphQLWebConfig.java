package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQLWebConfig {

  /**
   * GraphQL requests are read with a plain {@link ObjectMapper} instead of the application one,
   * whose {@code UNWRAP_ROOT_VALUE} setting is required by the REST API but makes the {@code
   * {"query": ...}} request body unreadable.
   */
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
  }
}
