package io.spring;

import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@Configuration
public class JacksonCustomizations {

  @Bean
  public JacksonModule realWorldModules() {
    return new RealWorldModules();
  }

  public static class RealWorldModules extends SimpleModule {
    public RealWorldModules() {
      addSerializer(Instant.class, new DateTimeSerializer());
    }
  }

  public static class DateTimeSerializer extends StdSerializer<Instant> {

    protected DateTimeSerializer() {
      super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext context) {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(Util.formatDateTime(value));
      }
    }
  }
}
