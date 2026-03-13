package io.spring.api;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Jackson message converters so that REST API types annotated with {@link JsonRootName}
 * are deserialized with root unwrapping, while other types (including Spring for GraphQL's {@code
 * SerializableGraphQlRequest}) are deserialized without root unwrapping.
 *
 * <p>This replaces the global {@code spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true}
 * property which broke the Spring for GraphQL HTTP handler.
 */
@Configuration
public class JacksonConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    MappingJackson2HttpMessageConverter defaultConverter = null;
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter) {
        defaultConverter = (MappingJackson2HttpMessageConverter) converter;
        break;
      }
    }

    if (defaultConverter != null) {
      ObjectMapper wrappingMapper = defaultConverter.getObjectMapper().copy();
      wrappingMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

      converters.add(
          0,
          new MappingJackson2HttpMessageConverter(wrappingMapper) {
            @Override
            public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
              return clazz.isAnnotationPresent(JsonRootName.class)
                  && super.canRead(clazz, mediaType);
            }

            @Override
            public boolean canRead(
                Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
              Class<?> clazz = resolveClass(type);
              if (clazz == null || !clazz.isAnnotationPresent(JsonRootName.class)) {
                return false;
              }
              return super.canRead(type, contextClass, mediaType);
            }

            @Override
            public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
              return false;
            }
          });
    }
  }

  private static Class<?> resolveClass(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      Type rawType = ((ParameterizedType) type).getRawType();
      if (rawType instanceof Class) {
        return (Class<?>) rawType;
      }
    }
    return null;
  }
}
