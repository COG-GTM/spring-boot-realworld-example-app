package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQLHttpConfig {

  /**
   * GraphQL request bodies are not wrapped in a root key, so they must not be read with the REST
   * API's {@code UNWRAP_ROOT_VALUE} setting.
   */
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, Jackson2ObjectMapperBuilder objectMapperBuilder) {
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(
            objectMapperBuilder
                .featuresToDisable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .build());
    return new GraphQlHttpHandler(webGraphQlHandler, converter);
  }
}
