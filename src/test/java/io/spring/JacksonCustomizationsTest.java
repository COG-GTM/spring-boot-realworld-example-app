package io.spring;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.StringWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  @Test
  void should_create_module() {
    JacksonCustomizations customizations = new JacksonCustomizations();
    Module module = customizations.realWorldModules();
    assertNotNull(module);
  }

  @Test
  void should_serialize_datetime() throws Exception {
    DateTime dateTime = new DateTime(2022, 1, 15, 10, 30, 0, DateTimeZone.UTC);
    JacksonCustomizations.DateTimeSerializer serializer =
        new JacksonCustomizations.DateTimeSerializer();

    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = new ObjectMapper().getSerializerProvider();

    serializer.serialize(dateTime, gen, provider);
    gen.flush();

    String expected = ISODateTimeFormat.dateTime().withZoneUTC().print(dateTime);
    assertEquals("\"" + expected + "\"", writer.toString());
  }

  @Test
  void should_serialize_null_datetime() throws Exception {
    JacksonCustomizations.DateTimeSerializer serializer =
        new JacksonCustomizations.DateTimeSerializer();

    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = new ObjectMapper().getSerializerProvider();

    serializer.serialize(null, gen, provider);
    gen.flush();

    assertEquals("null", writer.toString());
  }
}
