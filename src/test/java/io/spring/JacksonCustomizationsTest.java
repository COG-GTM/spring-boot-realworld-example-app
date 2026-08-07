package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Pins the wire format of serialized timestamps. The strings asserted here are exactly what the
 * previous Joda based serializer ({@code ISODateTimeFormat.dateTime().withZoneUTC()}) emitted.
 */
public class JacksonCustomizationsTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JacksonCustomizations.RealWorldModules());

  @Test
  public void should_serialize_instant_as_iso8601_utc_with_millis() throws Exception {
    assertEquals(
        "\"2021-01-01T12:00:00.000Z\"",
        objectMapper.writeValueAsString(instant("2021-01-01T12:00:00Z")));
    assertEquals(
        "\"2021-01-01T12:00:00.001Z\"",
        objectMapper.writeValueAsString(instant("2021-01-01T12:00:00.001Z")));
    assertEquals(
        "\"2021-01-01T12:00:00.100Z\"",
        objectMapper.writeValueAsString(instant("2021-01-01T12:00:00.1Z")));
    assertEquals(
        "\"2021-01-01T12:00:00.999Z\"",
        objectMapper.writeValueAsString(instant("2021-01-01T12:00:00.999Z")));
    assertEquals("\"1970-01-01T00:00:00.000Z\"", objectMapper.writeValueAsString(Instant.EPOCH));
    assertEquals(
        "\"1969-12-31T23:59:59.999Z\"", objectMapper.writeValueAsString(Instant.ofEpochMilli(-1L)));
  }

  @Test
  public void should_truncate_sub_millisecond_precision() throws Exception {
    assertEquals(
        "\"2021-01-01T12:00:00.123Z\"",
        objectMapper.writeValueAsString(instant("2021-01-01T12:00:00.123456789Z")));
  }

  @Test
  public void should_serialize_null_instant_as_null() throws Exception {
    assertEquals("null", objectMapper.writeValueAsString((Instant) null));
  }

  @Test
  public void should_serialize_article_timestamps_in_realworld_format() throws Exception {
    ArticleData articleData =
        new ArticleData(
            "id",
            "a-title",
            "a title",
            "desc",
            "body",
            false,
            0,
            instant("2021-01-01T12:00:00Z"),
            instant("2021-01-02T03:04:05.678Z"),
            Arrays.asList("java"),
            new ProfileData("uid", "user", "bio", "image", false));

    String json = objectMapper.writeValueAsString(articleData);

    assertEquals(true, json.contains("\"createdAt\":\"2021-01-01T12:00:00.000Z\""));
    assertEquals(true, json.contains("\"updatedAt\":\"2021-01-02T03:04:05.678Z\""));
  }

  private static Instant instant(String iso) {
    return Instant.parse(iso);
  }
}
