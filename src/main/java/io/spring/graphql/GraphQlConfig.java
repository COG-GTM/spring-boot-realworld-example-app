package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQlConfig {

  // The application enables Jackson's UNWRAP_ROOT_VALUE globally for the RealWorld REST API,
  // which would break parsing of GraphQL HTTP request bodies. Give the GraphQL HTTP handler a
  // dedicated converter backed by a plain ObjectMapper so request reading is unaffected.
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(new ObjectMapper());
    return new GraphQlHttpHandler(webGraphQlHandler, converter);
  }
}
