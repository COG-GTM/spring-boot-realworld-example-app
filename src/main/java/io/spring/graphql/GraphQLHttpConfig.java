package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQLHttpConfig {

  // The REST API enables UNWRAP_ROOT_VALUE globally; GraphQL request bodies are not wrapped.
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, Jackson2ObjectMapperBuilder objectMapperBuilder) {
    ObjectMapper objectMapper =
        objectMapperBuilder.featuresToDisable(DeserializationFeature.UNWRAP_ROOT_VALUE).build();
    return new GraphQlHttpHandler(
        webGraphQlHandler, new MappingJackson2HttpMessageConverter(objectMapper));
  }
}
