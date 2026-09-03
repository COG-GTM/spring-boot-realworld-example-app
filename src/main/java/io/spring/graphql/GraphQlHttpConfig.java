package io.spring.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The REST API relies on the global {@code UNWRAP_ROOT_VALUE} Jackson setting, which must not be
 * applied when Spring GraphQL reads the {@code /graphql} request body.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, JsonMapper jsonMapper) {
    JsonMapper graphQlMapper =
        jsonMapper.rebuild().disable(DeserializationFeature.UNWRAP_ROOT_VALUE).build();
    return new GraphQlHttpHandler(
        webGraphQlHandler, new JacksonJsonHttpMessageConverter(graphQlMapper));
  }
}
