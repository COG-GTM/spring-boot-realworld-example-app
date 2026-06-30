package io.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQLConfig {

  /**
   * The REST API relies on a globally configured {@code UNWRAP_ROOT_VALUE} ObjectMapper (see
   * application.properties and the {@code @JsonRootName} DTOs). Spring for GraphQL reuses that same
   * converter to read the GraphQL HTTP request body, which has no root wrapper, so requests fail to
   * deserialize. Back the GraphQL HTTP handler with a plain ObjectMapper to keep the two payload
   * conventions independent.
   */
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
  }
}
