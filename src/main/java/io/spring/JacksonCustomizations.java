package io.spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCustomizations {

  @Bean
  public Module realWorldModules() {
    return new RealWorldModules();
  }

  public static class RealWorldModules extends SimpleModule {
    public RealWorldModules() {
      addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
    }
  }

  public static class ZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

    protected ZonedDateTimeSerializer() {
      super(ZonedDateTime.class);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                value.withZoneSameInstant(java.time.ZoneOffset.UTC)));
      }
    }
  }
}
