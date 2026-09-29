package io.spring.api.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
public class RequestIdPropagationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private MockMvc mvc;

  @Test
  public void should_put_request_id_on_every_log_line_of_the_request(CapturedOutput output)
      throws Exception {
    String requestId = "propagation-test-42";

    mvc.perform(get("/articles").header(RequestIdFilter.HEADER, requestId))
        .andExpect(status().isOk())
        .andExpect(header().string(RequestIdFilter.HEADER, requestId));

    List<JsonNode> requestLines =
        jsonLines(output).stream()
            .filter(
                line ->
                    line.path("logger_name").asText().startsWith("io.spring.infrastructure")
                        || line.path("logger_name")
                            .asText()
                            .equals(RequestIdFilter.class.getName()))
            .collect(Collectors.toList());

    assertFalse(requestLines.isEmpty(), "expected SQL and access log lines for the request");
    requestLines.forEach(
        line -> assertEquals(requestId, line.path("requestId").asText(), line.toString()));
    assertTrue(
        requestLines.stream()
            .anyMatch(
                line -> line.path("logger_name").asText().startsWith("io.spring.infrastructure")),
        "expected downstream (MyBatis) log lines to carry the request id");
  }

  @Test
  public void should_expose_health_and_readiness_without_authentication() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  private static List<JsonNode> jsonLines(CapturedOutput output) {
    return output
        .getOut()
        .lines()
        .filter(line -> line.startsWith("{"))
        .map(
            line -> {
              try {
                return MAPPER.readTree(line);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            })
        .collect(Collectors.toList());
  }
}
