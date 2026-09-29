package io.spring.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest
public class OpenApiDocumentTest {
  private static final Path OPENAPI = Paths.get("docs", "openapi.yaml");
  private static final Path DIVERGENCES = Paths.get("docs", "realworld-api-divergences.md");
  private static final Set<String> HTTP_METHODS =
      new HashSet<>(Arrays.asList("get", "put", "post", "delete", "patch", "head", "options"));

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Test
  public void openapi_document_describes_exactly_the_rest_controller_mappings() throws IOException {
    Set<String> implemented = new TreeSet<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handlerMethod) -> {
              if (!handlerMethod.getBeanType().getPackage().getName().equals("io.spring.api")) {
                return;
              }
              for (String pattern : info.getPatternValues()) {
                info.getMethodsCondition()
                    .getMethods()
                    .forEach(method -> implemented.add(method.name() + " " + pattern));
              }
            });

    assertEquals(implemented, documentedOperations());
  }

  @Test
  public void every_referenced_divergence_is_documented() throws IOException {
    String report = new String(Files.readAllBytes(DIVERGENCES), StandardCharsets.UTF_8);
    Set<String> documented = new TreeSet<>();
    Matcher matcher = Pattern.compile("(?m)^\\| (D\\d{2}) \\|").matcher(report);
    while (matcher.find()) {
      documented.add(matcher.group(1));
    }

    Set<String> referenced = new TreeSet<>();
    forEachOperation(
        (key, operation) -> {
          Object ids = operation.get("x-realworld-divergences");
          assertTrue(ids instanceof List, key + " has no x-realworld-divergences list");
          for (Object id : (List<?>) ids) {
            referenced.add(id.toString());
          }
        });

    referenced.removeAll(documented);
    assertTrue(referenced.isEmpty(), "Undocumented divergence ids: " + referenced);
  }

  private Set<String> documentedOperations() throws IOException {
    Set<String> documented = new TreeSet<>();
    forEachOperation((key, operation) -> documented.add(key));
    return documented;
  }

  @SuppressWarnings("unchecked")
  private void forEachOperation(OperationConsumer consumer) throws IOException {
    Map<String, Object> document;
    try (InputStream in = Files.newInputStream(OPENAPI)) {
      document = new Yaml().load(in);
    }
    Map<String, Map<String, Object>> paths =
        (Map<String, Map<String, Object>>) document.get("paths");
    for (Map.Entry<String, Map<String, Object>> path : paths.entrySet()) {
      for (Map.Entry<String, Object> operation : path.getValue().entrySet()) {
        if (HTTP_METHODS.contains(operation.getKey())) {
          consumer.accept(
              operation.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey(),
              (Map<String, Object>) operation.getValue());
        }
      }
    }
  }

  private interface OperationConsumer {
    void accept(String key, Map<String, Object> operation);
  }
}
