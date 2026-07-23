package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The application-wide ObjectMapper enables UNWRAP_ROOT_VALUE for the REST API, which would break
 * parsing of GraphQL requests, so the GraphQL HTTP transport uses its own plain ObjectMapper.
 */
@Configuration
public class GraphQLTransportConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
  }
}
