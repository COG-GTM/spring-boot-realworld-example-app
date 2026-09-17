package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GraphQLHttpConfiguration {

  /**
   * GraphQL requests are plain JSON documents, so they must be read with a mapper that does not
   * apply the application wide {@code UNWRAP_ROOT_VALUE} deserialization feature used by the REST
   * API. Every other customization of the application mapper is kept.
   */
  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(
            objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE));
    converter.setSupportedMediaTypes(
        List.of(MediaType.APPLICATION_GRAPHQL_RESPONSE, MediaType.APPLICATION_JSON));
    return new GraphQlHttpHandler(webGraphQlHandler, converter);
  }
}
