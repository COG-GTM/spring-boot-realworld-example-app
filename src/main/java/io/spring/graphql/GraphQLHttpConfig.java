package io.spring.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class GraphQLHttpConfig {

  // The app-wide mapper unwraps root values for the REST DTOs; GraphQL request bodies are not
  // wrapped, so /graphql needs its own mapper.
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    return new GraphQlHttpHandler(
        webGraphQlHandler, new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()));
  }
}
