package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Overrides the default Spring GraphQL HTTP endpoint to use an ObjectMapper without
 * UNWRAP_ROOT_VALUE / WRAP_ROOT_VALUE. The REST API requires those settings for the RealWorld spec
 * (e.g. {@code {"user":{...}}}), but GraphQL request/response bodies are plain JSON without root
 * wrappers.
 */
@Configuration
public class GraphQLConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(
      WebGraphQlHandler webGraphQlHandler, ObjectMapper objectMapper) {
    ObjectMapper graphqlMapper = objectMapper.copy();
    graphqlMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    graphqlMapper.disable(SerializationFeature.WRAP_ROOT_VALUE);
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(graphqlMapper);
    return new GraphQlHttpHandler(webGraphQlHandler, converter);
  }

  @Bean
  public RouterFunction<ServerResponse> graphQlRouterFunction(
      GraphQlHttpHandler graphQlHttpHandler) {
    return RouterFunctions.route()
        .POST(
            "/graphql",
            RequestPredicates.contentType(
                MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/graphql+json")),
            graphQlHttpHandler::handleRequest)
        .build();
  }
}
