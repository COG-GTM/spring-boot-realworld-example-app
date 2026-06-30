package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Wires Spring for GraphQL's HTTP handler with a dedicated Jackson converter. The REST layer relies
 * on the global {@code DeserializationFeature.UNWRAP_ROOT_VALUE} (combined with
 * {@code @JsonRootName}) to parse root-wrapped payloads, but that feature breaks parsing of the
 * GraphQL request envelope. This handler uses a copy of the application {@link ObjectMapper} with
 * only that feature disabled.
 */
@Configuration
public class GraphQlConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    ObjectMapper graphQlMapper =
        objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(graphQlMapper));
  }
}
