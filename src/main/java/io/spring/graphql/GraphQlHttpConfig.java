package io.spring.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The REST API's ObjectMapper unwraps a root value ({"user": {...}}), which breaks plain GraphQL
 * request bodies ({"query": ...}). Give the /graphql endpoint its own JSON converter.
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
