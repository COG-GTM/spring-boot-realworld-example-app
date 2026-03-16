package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Overrides the auto-configured GraphQL RouterFunction so that the /graphql endpoint uses an
 * ObjectMapper without UNWRAP_ROOT_VALUE. The global ObjectMapper has UNWRAP_ROOT_VALUE=true for
 * the REST API (which uses @JsonRootName), but Spring GraphQL's request parsing fails with that
 * setting enabled.
 */
@Configuration
public class GraphQlJacksonConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public RouterFunction<ServerResponse> graphQlCustomRouterFunction(
      GraphQlHttpHandler handler, ObjectMapper objectMapper) {
    ObjectMapper graphQlMapper =
        objectMapper.copy().disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    List<HttpMessageConverter<?>> converters =
        List.of(new MappingJackson2HttpMessageConverter(graphQlMapper));

    return RouterFunctions.route()
        .POST(
            "/graphql",
            request ->
                handler.handleRequest(ServerRequest.create(request.servletRequest(), converters)))
        .build();
  }
}
