package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers a read-only Jackson converter dedicated to Spring for GraphQL request bodies.
 *
 * <p>The application enables {@code spring.jackson.deserialization.UNWRAP_ROOT_VALUE} globally so
 * its {@code @JsonRootName}-wrapped REST payloads (e.g. {@code {"user": {...}}}) bind correctly.
 * Spring for GraphQL (used by DGS) deserializes the transport request into {@link
 * SerializableGraphQlRequest} through the same MVC converters, and root unwrapping breaks that
 * parse. This converter handles only that type with an ObjectMapper that does not unwrap, leaving
 * all other reads and every write to the default converters.
 */
@Configuration
public class GraphQlHttpMessageConverterConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    converters.add(0, new GraphQlRequestConverter(objectMapper));
  }

  private static class GraphQlRequestConverter extends MappingJackson2HttpMessageConverter {

    GraphQlRequestConverter(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
      return supportsGraphQlRequest(clazz) && super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
      return type instanceof Class
          && supportsGraphQlRequest((Class<?>) type)
          && super.canRead(type, contextClass, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return false;
    }

    private boolean supportsGraphQlRequest(Class<?> clazz) {
      return SerializableGraphQlRequest.class.isAssignableFrom(clazz);
    }
  }
}
