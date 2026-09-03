package io.spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.SerializableGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class JacksonCustomizations implements WebMvcConfigurer {

  @Bean
  public Module realWorldModules() {
    return new RealWorldModules();
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper graphQlObjectMapper = new ObjectMapper();
    graphQlObjectMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    GraphQlMessageConverter graphQlMessageConverter =
        new GraphQlMessageConverter(graphQlObjectMapper);
    graphQlMessageConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
    converters.add(0, graphQlMessageConverter);
  }

  private static class GraphQlMessageConverter extends MappingJackson2HttpMessageConverter {
    private GraphQlMessageConverter(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
      return SerializableGraphQlRequest.class.isAssignableFrom(clazz)
          && (mediaType == null
              || getSupportedMediaTypes().stream()
                  .anyMatch(supported -> supported.isCompatibleWith(mediaType)));
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
      return type instanceof Class && canRead((Class<?>) type, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return false;
    }
  }

  public static class RealWorldModules extends SimpleModule {
    public RealWorldModules() {
      addSerializer(DateTime.class, new DateTimeSerializer());
    }
  }

  public static class DateTimeSerializer extends StdSerializer<DateTime> {

    protected DateTimeSerializer() {
      super(DateTime.class);
    }

    @Override
    public void serialize(DateTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(ISODateTimeFormat.dateTime().withZoneUTC().print(value));
      }
    }
  }
}
