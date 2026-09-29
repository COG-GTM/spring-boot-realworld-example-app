package io.spring.infrastructure.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {"logging.level.io.spring.http=INFO", "logging.level.io.spring.service=DEBUG"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
public class CorrelationIdPropagationTest {
  @Autowired private MockMvc mvc;

  @Test
  public void should_propagate_correlation_id_through_service_layer(CapturedOutput output)
      throws Exception {
    mvc.perform(get("/tags").header("X-Correlation-Id", "it-corr-42"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Correlation-Id", "it-corr-42"));

    String out = output.getOut();
    assertThat(out, containsString("[it-corr-42]"));
    assertThat(out, containsString("-> TagsQueryService.allTags"));
    assertThat(out, containsString("GET /tags -> 200"));
  }
}
