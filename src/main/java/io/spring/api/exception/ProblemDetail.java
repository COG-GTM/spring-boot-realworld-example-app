package io.spring.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * RFC 7807 "Problem Details for HTTP APIs" response body, served as {@code
 * application/problem+json}.
 *
 * <p>{@code errors} is an extension member carrying per-field validation messages in the RealWorld
 * shape ({@code {"field": ["message", ...]}}).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type", "title", "status", "detail", "instance", "errors"})
public class ProblemDetail {
  public static final URI BLANK_TYPE = URI.create("about:blank");

  private final URI type;
  private final String title;
  private final Integer status;
  private final String detail;
  private final URI instance;
  private final Map<String, List<String>> errors;

  public static ProblemDetailBuilder forStatus(HttpStatus status) {
    return builder().type(BLANK_TYPE).title(status.getReasonPhrase()).status(status.value());
  }

  public static ProblemDetailBuilder forType(ProblemType problemType) {
    return builder()
        .type(problemType.getType())
        .title(problemType.getTitle())
        .status(problemType.getStatus().value());
  }
}
