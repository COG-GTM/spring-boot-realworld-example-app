package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The REST API relies on a global {@code UNWRAP_ROOT_VALUE} ObjectMapper (DTOs are annotated with
 * {@code @JsonRootName}). Spring for GraphQL reads the HTTP request body into {@link
 * SerializableGraphQlRequest} using the same converters, and unwrapping breaks that parsing. This
 * registers a read-only converter, restricted to {@link SerializableGraphQlRequest}, that does not
 * unwrap, leaving all other JSON handling untouched.
 */
@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper objectMapper =
        new ObjectMapper()
            .disable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    converters.add(0, new GraphQlRequestHttpMessageConverter(objectMapper));
  }

  private static class GraphQlRequestHttpMessageConverter
      extends MappingJackson2HttpMessageConverter {

    GraphQlRequestHttpMessageConverter(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
      return type == SerializableGraphQlRequest.class
          && super.canRead(type, contextClass, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return false;
    }
  }
}
