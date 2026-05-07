package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers a Jackson HTTP message converter that handles only Spring GraphQL request/response
 * types. The application-wide ObjectMapper has {@code UNWRAP_ROOT_VALUE} / {@code WRAP_ROOT_VALUE}
 * enabled to support wrapped REST payloads (e.g. {@code {"user": {...}}}); Spring GraphQL's {@code
 * SerializableGraphQlRequest} expects an unwrapped payload, so we give it a dedicated converter
 * that does not toggle those features.
 */
@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphQlMapper =
        new ObjectMapper()
            .disable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .disable(SerializationFeature.WRAP_ROOT_VALUE);

    MappingJackson2HttpMessageConverter graphQlConverter =
        new MappingJackson2HttpMessageConverter(graphQlMapper) {
          @Override
          public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return isGraphQlType(type.getTypeName())
                && super.canRead(type, contextClass, mediaType);
          }

          @Override
          public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return isGraphQlType(clazz.getName()) && super.canWrite(clazz, mediaType);
          }

          private boolean isGraphQlType(String typeName) {
            return typeName.startsWith("org.springframework.graphql.")
                || typeName.startsWith("graphql.");
          }
        };

    converters.add(0, graphQlConverter);
  }
}
