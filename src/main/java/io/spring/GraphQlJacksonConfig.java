package io.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers a Jackson message converter without UNWRAP_ROOT_VALUE for Spring GraphQL types.
 *
 * <p>The REST API requires {@code UNWRAP_ROOT_VALUE=true} globally (requests are wrapped like
 * {@code {"user":{...}}}), but Spring GraphQL's {@code SerializableGraphQlRequest} does not use
 * root wrapping. This converter is placed first so it handles GraphQL deserialization with a clean
 * ObjectMapper while the default converter (with UNWRAP_ROOT_VALUE) handles REST API requests.
 */
@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  private final Jackson2ObjectMapperBuilder mapperBuilder;

  public GraphQlJacksonConfig(Jackson2ObjectMapperBuilder mapperBuilder) {
    this.mapperBuilder = mapperBuilder;
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphQlMapper = mapperBuilder.build();
    graphQlMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);

    MappingJackson2HttpMessageConverter graphQlConverter =
        new MappingJackson2HttpMessageConverter(graphQlMapper) {

          private boolean isGraphQlType(Type type) {
            return type instanceof Class<?> clazz
                && clazz.getPackageName().startsWith("org.springframework.graphql");
          }

          @Override
          public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return isGraphQlType(type) && super.canRead(type, contextClass, mediaType);
          }

          @Override
          public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return clazz.getPackageName().startsWith("org.springframework.graphql")
                && super.canRead(clazz, mediaType);
          }

          @Override
          public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
          }

          @Override
          public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
            return false;
          }
        };

    converters.add(0, graphQlConverter);
  }
}
