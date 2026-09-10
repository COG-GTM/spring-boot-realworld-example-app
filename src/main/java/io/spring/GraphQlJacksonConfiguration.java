package io.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GraphQlJacksonConfiguration implements WebMvcConfigurer {

  private final ObjectMapper objectMapper;

  public GraphQlJacksonConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
                    objectMappers ->
                        objectMappers.put(MediaType.APPLICATION_JSON, graphQlObjectMapper)));
  }
}
