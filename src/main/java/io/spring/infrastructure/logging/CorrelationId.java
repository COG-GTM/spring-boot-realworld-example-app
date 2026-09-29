package io.spring.infrastructure.logging;

import java.util.Optional;
import org.slf4j.MDC;

public final class CorrelationId {
  public static final String MDC_KEY = "correlationId";
  public static final String REQUEST_ATTRIBUTE = CorrelationId.class.getName();

  private CorrelationId() {}

  public static Optional<String> current() {
    return Optional.ofNullable(MDC.get(MDC_KEY));
  }
}
