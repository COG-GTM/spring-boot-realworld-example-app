package io.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphqlMapper = new ObjectMapper();

    MappingJackson2HttpMessageConverter graphqlConverter =
        new MappingJackson2HttpMessageConverter(graphqlMapper) {
          @Override
          public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            if (!canRead(mediaType)) {
              return false;
            }
            if (type instanceof Class) {
              return SerializableGraphQlRequest.class.isAssignableFrom((Class<?>) type);
            }
            return false;
          }

          @Override
          public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
          }
        };

    converters.add(0, graphqlConverter);
  }
}
