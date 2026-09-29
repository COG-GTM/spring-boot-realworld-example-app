package io.spring.api;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Loads {@code static/openapi.yaml} and validates real HTTP interactions against it.
 *
 * <p>Every request sent through {@link #validating()} or {@link #validatingResponseOnly()} must hit
 * a documented operation, and its response must match the documented status code and schema. The
 * {@code METHOD path status} triples seen are recorded so a test can assert that every documented
 * response was exercised at least once.
 */
final class OpenApiContract {
  static final String SPEC_RESOURCE = "static/openapi.yaml";

  private final String specContent;
  private final SwaggerParseResult parseResult;
  private final List<Operation> operations;
  private final OpenApiInteractionValidator strictValidator;
  private final OpenApiInteractionValidator responseOnlyValidator;
  private final Set<String> exercised = ConcurrentHashMap.newKeySet();

  private OpenApiContract(String specContent) {
    this.specContent = specContent;
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    this.parseResult = new OpenAPIV3Parser().readContents(specContent, null, options);
    this.operations = parseResult.getOpenAPI() == null ? List.of() : operationsOf(parseResult);
    this.strictValidator = validator(new LevelResolver.Builder());
    this.responseOnlyValidator =
        validator(
            new LevelResolver.Builder()
                .withLevel("validation.request", ValidationReport.Level.IGNORE));
  }

  static OpenApiContract load() {
    try {
      return new OpenApiContract(
          StreamUtils.copyToString(
              new ClassPathResource(SPEC_RESOURCE).getInputStream(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  String specContent() {
    return specContent;
  }

  List<String> parseMessages() {
    return parseResult.getMessages() == null ? List.of() : parseResult.getMessages();
  }

  OpenAPI openApi() {
    return parseResult.getOpenAPI();
  }

  List<Operation> operations() {
    return operations;
  }

  /** Validates both the request and the response against the spec. */
  Filter validating() {
    return new ValidationFilter(strictValidator);
  }

  /**
   * Validates only the response. Use for requests that deliberately violate the spec (missing
   * token, invalid body) to check how the server rejects them.
   */
  Filter validatingResponseOnly() {
    return new ValidationFilter(responseOnlyValidator);
  }

  /** Documented {@code METHOD path status} triples that no validated interaction has produced. */
  Set<String> unexercisedResponses() {
    Set<String> documented = new TreeSet<>();
    for (Operation operation : operations) {
      for (String status : operation.statuses) {
        documented.add(operation.key() + " " + status);
      }
    }
    documented.removeAll(exercised);
    return documented;
  }

  private OpenApiInteractionValidator validator(LevelResolver.Builder levels) {
    return OpenApiInteractionValidator.createForInlineApiSpecification(specContent)
        .withLevelResolver(levels.withDefaultLevel(ValidationReport.Level.ERROR).build())
        .build();
  }

  private static List<Operation> operationsOf(SwaggerParseResult result) {
    List<Operation> operations = new ArrayList<>();
    result
        .getOpenAPI()
        .getPaths()
        .forEach(
            (path, item) ->
                item.readOperationsMap()
                    .forEach(
                        (method, operation) ->
                            operations.add(
                                new Operation(
                                    method.name(),
                                    path,
                                    operation.getResponses().keySet(),
                                    operation.getRequestBody() != null))));
    // Prefer literal segments over templates, e.g. /articles/feed over /articles/{slug}.
    operations.sort(Comparator.comparingInt(operation -> operation.templateVariables()));
    return operations;
  }

  private Optional<Operation> findOperation(String method, String path) {
    return operations.stream()
        .filter(operation -> operation.method.equals(method) && operation.matches(path))
        .findFirst();
  }

  static final class Operation {
    final String method;
    final String path;
    final Set<String> statuses;
    final boolean hasRequestBody;
    private final Pattern pathPattern;

    Operation(String method, String path, Set<String> statuses, boolean hasRequestBody) {
      this.method = method;
      this.path = path;
      this.statuses = new TreeSet<>(statuses);
      this.hasRequestBody = hasRequestBody;
      this.pathPattern =
          Pattern.compile(
              Arrays.stream(path.split("/", -1))
                  .map(segment -> segment.startsWith("{") ? "[^/]+" : Pattern.quote(segment))
                  .collect(Collectors.joining("/")));
    }

    String key() {
      return method + " " + path;
    }

    boolean matches(String requestPath) {
      return pathPattern.matcher(requestPath).matches();
    }

    int templateVariables() {
      return path.length() - path.replace("{", "").length();
    }

    /** The path with every template variable replaced by {@code value}. */
    String pathWith(String value) {
      return path.replaceAll("\\{[^/}]+}", value);
    }
  }

  private final class ValidationFilter implements Filter {
    private final OpenApiInteractionValidator validator;

    ValidationFilter(OpenApiInteractionValidator validator) {
      this.validator = validator;
    }

    @Override
    public Response filter(
        FilterableRequestSpecification request,
        FilterableResponseSpecification responseSpec,
        FilterContext context) {
      Response response = context.next(request, responseSpec);

      String method = request.getMethod().toUpperCase();
      String path = URI.create(request.getURI()).getRawPath();
      Operation operation =
          findOperation(method, path)
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "Request to undocumented endpoint " + method + " " + path));

      ValidationReport report = validator.validate(toRequest(request, path), toResponse(response));
      if (report.hasErrors()) {
        throw new AssertionError(
            String.format(
                "%s %s -> %d does not match %s:%n%s%nResponse body: %s",
                method,
                path,
                response.getStatusCode(),
                SPEC_RESOURCE,
                SimpleValidationReportFormat.getInstance().apply(report),
                response.asString()));
      }
      exercised.add(operation.key() + " " + response.getStatusCode());
      return response;
    }

    private SimpleRequest toRequest(FilterableRequestSpecification request, String path) {
      SimpleRequest.Builder builder = new SimpleRequest.Builder(request.getMethod(), path);
      for (Header header : request.getHeaders()) {
        builder.withHeader(header.getName(), header.getValue());
      }
      for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
        builder.withQueryParam(param.getKey(), param.getValue());
      }
      Object body = request.getBody();
      if (body != null) {
        builder.withBody(body.toString());
      }
      return builder.build();
    }

    private SimpleResponse toResponse(Response response) {
      SimpleResponse.Builder builder = SimpleResponse.Builder.status(response.getStatusCode());
      for (Header header : response.getHeaders()) {
        builder.withHeader(header.getName(), header.getValue());
      }
      String body = response.asString();
      if (!body.isEmpty()) {
        builder.withBody(body);
      }
      return builder.build();
    }
  }

  static String describe(Set<String> entries) {
    return entries.stream().collect(Collectors.joining(System.lineSeparator() + "  ", "  ", ""));
  }
}
