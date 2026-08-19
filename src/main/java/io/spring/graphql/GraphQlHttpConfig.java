package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The application-wide ObjectMapper enables UNWRAP_ROOT_VALUE for the RealWorld REST API, which
 * breaks deserialization of standard GraphQL HTTP requests. This handler uses a plain ObjectMapper
 * for the /graphql endpoint.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
  }
}
