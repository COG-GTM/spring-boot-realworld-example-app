package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  @Test
  public void should_serialize_errors_by_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("obj", "email", "code", "already exist"),
                new FieldErrorResource("obj", "email", "code", "another message"),
                new FieldErrorResource("obj", "username", "code", "invalid username")));

    assertThat(ErrorResource.class.isAnnotationPresent(JsonSerialize.class), is(true));
    JsonNode json =
        new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(errorResource));

    assertThat(json.path("errors").path("email").size(), is(2));
    assertThat(json.path("errors").path("email").get(0).asText(), is("already exist"));
    assertThat(json.path("errors").path("username").size(), is(1));
    assertThat(json.path("errors").path("username").get(0).asText(), is("invalid username"));
  }
}
