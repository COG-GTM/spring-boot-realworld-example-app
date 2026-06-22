package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQlConfig {

  /**
   * The REST API relies on a global Jackson {@code UNWRAP_ROOT_VALUE} feature to unwrap request
   * bodies such as {@code {"user": {...}}}. Spring for GraphQL deserializes the raw {@code
   * {"query": ...}} payload into {@code SerializableGraphQlRequest}, which is not root-wrapped, so
   * it must use a converter that does not unwrap (or wrap) the root value.
   */
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    ObjectMapper graphQlObjectMapper =
        objectMapper
            .copy()
            .disable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .disable(SerializationFeature.WRAP_ROOT_VALUE);
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(graphQlObjectMapper));
  }
}
