package io.spring.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Uses a dedicated ObjectMapper for the GraphQL HTTP transport so that the application-wide
 * UNWRAP_ROOT_VALUE Jackson setting (required by the REST API) does not break deserialization of
 * standard GraphQL request bodies.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler,
        new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()));
  }
}
