package io.spring.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The REST API's shared ObjectMapper unwraps root values (spring.jackson.deserialization
 * .UNWRAP_ROOT_VALUE), which breaks parsing of GraphQL request bodies. Give the GraphQL endpoint
 * its own converter that is not affected by the spring.jackson.* properties.
 */
@Configuration
public class GraphQLHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler,
        new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()));
  }
}
