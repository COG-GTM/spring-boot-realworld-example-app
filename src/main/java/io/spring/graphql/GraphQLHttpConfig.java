package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * The REST API relies on {@code spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true}, which would
 * break decoding of the standard {@code {"query": ...}} GraphQL request envelope. The GraphQL
 * endpoint therefore gets its own converter with root-value unwrapping disabled.
 */
@Configuration
public class GraphQLHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    ObjectMapper graphqlMapper =
        objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(graphqlMapper));
  }
}
