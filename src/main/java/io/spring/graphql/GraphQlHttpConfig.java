package io.spring.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

/**
 * Provides a GraphQL HTTP handler backed by a plain JSON converter, isolated from the
 * application-wide Jackson settings (such as root value unwrapping) that would otherwise break
 * parsing of GraphQL request payloads.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(webGraphQlHandler, new JacksonJsonHttpMessageConverter());
  }
}
