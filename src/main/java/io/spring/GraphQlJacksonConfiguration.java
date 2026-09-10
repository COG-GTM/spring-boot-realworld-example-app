package io.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GraphQlJacksonConfiguration implements WebMvcConfigurer {

  private final ObjectMapper objectMapper;

  public GraphQlJacksonConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  @Order(-1)
  RouterFunction<ServerResponse> graphQlJsonRouterFunction(
      ObjectProvider<GraphQlHttpHandler> graphQlHttpHandlerProvider,
      @Value("${spring.graphql.http.path:/graphql}") String graphQlPath) {
    GraphQlHttpHandler graphQlHttpHandler = graphQlHttpHandlerProvider.getIfAvailable();
    if (graphQlHttpHandler == null) {
      return RouterFunctions.route(request -> false, request -> ServerResponse.notFound().build());
    }
    return RouterFunctions.route(
        RequestPredicates.POST(graphQlPath)
            .and(RequestPredicates.contentType(new MediaType("application", "graphql+json"))),
        graphQlHttpHandler::handleRequest);
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphQlObjectMapper =
        objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    converters.stream()
        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
        .map(MappingJackson2HttpMessageConverter.class::cast)
        .findFirst()
        .ifPresent(
            converter ->
                converter.registerObjectMappersForType(
                    SerializableGraphQlRequest.class,
                    objectMappers -> {
                      objectMappers.put(MediaType.APPLICATION_JSON, graphQlObjectMapper);
                      objectMappers.put(
                          new MediaType("application", "*+json"), graphQlObjectMapper);
                    }));
  }
}
