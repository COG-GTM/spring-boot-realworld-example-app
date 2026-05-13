package io.spring.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  @Autowired private ObjectMapper objectMapper;

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphqlMapper = objectMapper.copy();
    graphqlMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);

    MappingJackson2HttpMessageConverter graphqlConverter =
        new MappingJackson2HttpMessageConverter(graphqlMapper) {
          @Override
          public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            String typeName = type.getTypeName();
            if (typeName.contains("GraphQl") || typeName.contains("graphql")) {
              return super.canRead(type, contextClass, mediaType);
            }
            return false;
          }
        };

    converters.add(0, graphqlConverter);
  }
}
