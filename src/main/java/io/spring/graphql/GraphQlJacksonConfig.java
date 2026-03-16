package io.spring.graphql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures a high-priority Jackson message converter for Spring GraphQL requests that does not
 * use UNWRAP_ROOT_VALUE. The global ObjectMapper has UNWRAP_ROOT_VALUE=true (required by the REST
 * API's @JsonRootName DTOs), but this breaks Spring GraphQL's SerializableGraphQlRequest parsing.
 */
@Configuration
public class GraphQlJacksonConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphQlMapper = Jackson2ObjectMapperBuilder.json().build();
    graphQlMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    converters.add(0, new GraphQlHttpMessageConverter(graphQlMapper));
  }

  private static class GraphQlHttpMessageConverter extends MappingJackson2HttpMessageConverter {

    GraphQlHttpMessageConverter(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public boolean canRead(
        Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
      if (type instanceof Class<?> clazz && isGraphQlType(clazz)) {
        return super.canRead(type, contextClass, mediaType);
      }
      return false;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
      if (isGraphQlType(clazz)) {
        return super.canRead(clazz, mediaType);
      }
      return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
      if (isGraphQlType(clazz)) {
        return super.canWrite(clazz, mediaType);
      }
      return false;
    }

    private static boolean isGraphQlType(Class<?> clazz) {
      String packageName = clazz.getPackageName();
      return packageName.startsWith("org.springframework.graphql");
    }
  }
}
