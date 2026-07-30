package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The application enables UNWRAP_ROOT_VALUE globally for the RealWorld REST payloads, which breaks
 * deserialization of GraphQL requests. This handler uses a plain ObjectMapper for the /graphql
 * endpoint.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
  }
}
