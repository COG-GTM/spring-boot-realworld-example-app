package io.spring.infrastructure.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.logging")
public class LoggingProperties {
  private final Correlation correlation = new Correlation();
  private final Http http = new Http();
  private final Service service = new Service();

  @Getter
  @Setter
  public static class Correlation {
    private String headerName = "X-Correlation-Id";
    private boolean acceptIncoming = true;
  }

  @Getter
  @Setter
  public static class Http {
    private boolean enabled = true;
    private boolean includeHeaders = false;
    private boolean includePayload = false;
    private int maxPayloadLength = 1000;
    private List<String> excludePaths = new ArrayList<>(Arrays.asList("/graphiql/**"));
    private List<String> maskedHeaders =
        new ArrayList<>(Arrays.asList("Authorization", "Cookie", "Set-Cookie"));
    private List<String> maskedFields = new ArrayList<>(Arrays.asList("password", "token"));
  }

  @Getter
  @Setter
  public static class Service {
    private boolean enabled = true;
  }
}
