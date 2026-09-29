package io.spring.infrastructure.logging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

public class RequestResponseLoggingFilter extends OncePerRequestFilter {
  static final String LOGGER_NAME = "io.spring.http";
  private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
  private static final String MASK = "***";

  private final LoggingProperties.Http properties;
  private final Set<String> maskedHeaders;
  private final Pattern maskedFieldsPattern;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public RequestResponseLoggingFilter(LoggingProperties.Http properties) {
    this.properties = properties;
    this.maskedHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    this.maskedHeaders.addAll(properties.getMaskedHeaders());
    this.maskedFieldsPattern = buildMaskedFieldsPattern(properties.getMaskedFields());
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return !log.isInfoEnabled()
        || properties.getExcludePaths().stream().anyMatch(p -> pathMatcher.match(p, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    HttpServletRequest requestToUse = request;
    ContentCachingResponseWrapper responseWrapper = null;
    HttpServletResponse responseToUse = response;
    if (properties.isIncludePayload()) {
      requestToUse = new ContentCachingRequestWrapper(request, properties.getMaxPayloadLength());
      responseWrapper = new ContentCachingResponseWrapper(response);
      responseToUse = responseWrapper;
    }

    long start = System.nanoTime();
    try {
      filterChain.doFilter(requestToUse, responseToUse);
    } finally {
      long durationMs = (System.nanoTime() - start) / 1_000_000;
      logExchange(requestToUse, responseToUse, durationMs);
      if (responseWrapper != null) {
        responseWrapper.copyBodyToResponse();
      }
    }
  }

  private void logExchange(
      HttpServletRequest request, HttpServletResponse response, long durationMs) {
    StringBuilder message = new StringBuilder();
    message
        .append(request.getMethod())
        .append(' ')
        .append(requestPath(request))
        .append(" -> ")
        .append(response.getStatus())
        .append(" (")
        .append(durationMs)
        .append(" ms)");

    if (properties.isIncludeHeaders()) {
      message.append(" requestHeaders=").append(requestHeaders(request));
      message.append(" responseHeaders=").append(responseHeaders(response));
    }
    if (request instanceof ContentCachingRequestWrapper) {
      ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
      appendPayload(
          message,
          "requestBody",
          request.getContentType(),
          wrapper.getContentAsByteArray(),
          request.getCharacterEncoding());
    }
    if (response instanceof ContentCachingResponseWrapper) {
      ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
      appendPayload(
          message,
          "responseBody",
          response.getContentType(),
          wrapper.getContentAsByteArray(),
          response.getCharacterEncoding());
    }

    if (response.getStatus() >= 500) {
      log.warn(message.toString());
    } else {
      log.info(message.toString());
    }
  }

  private String requestPath(HttpServletRequest request) {
    String query = request.getQueryString();
    return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
  }

  private Map<String, String> requestHeaders(HttpServletRequest request) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (String name : Collections.list(request.getHeaderNames())) {
      headers.put(name, maskHeader(name, Collections.list(request.getHeaders(name))));
    }
    return headers;
  }

  private Map<String, String> responseHeaders(HttpServletResponse response) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (String name : new TreeSet<>(response.getHeaderNames())) {
      headers.put(name, maskHeader(name, response.getHeaders(name)));
    }
    return headers;
  }

  private String maskHeader(String name, Collection<String> values) {
    return maskedHeaders.contains(name) ? MASK : String.join(",", values);
  }

  private void appendPayload(
      StringBuilder message, String label, String contentType, byte[] content, String encoding) {
    if (content.length == 0) {
      return;
    }
    message.append(' ').append(label).append('=');
    if (!isTextual(contentType)) {
      message.append("[").append(content.length).append(" bytes ").append(contentType).append("]");
      return;
    }
    int length = Math.min(content.length, properties.getMaxPayloadLength());
    String body = new String(content, 0, length, charset(encoding));
    message.append(maskFields(body).replaceAll("[\\r\\n]+", " "));
    if (content.length > length) {
      message.append("...[truncated ").append(content.length - length).append(" bytes]");
    }
  }

  String maskFields(String body) {
    if (maskedFieldsPattern == null) {
      return body;
    }
    Matcher matcher = maskedFieldsPattern.matcher(body);
    return matcher.replaceAll("$1\"" + MASK + "\"");
  }

  private static Pattern buildMaskedFieldsPattern(List<String> fields) {
    if (fields.isEmpty()) {
      return null;
    }
    String names = fields.stream().map(Pattern::quote).collect(Collectors.joining("|"));
    return Pattern.compile(
        "(\"(?:" + names + ")\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"", Pattern.CASE_INSENSITIVE);
  }

  private static boolean isTextual(String contentType) {
    if (contentType == null) {
      return false;
    }
    String type = contentType.toLowerCase();
    return type.startsWith("text/")
        || type.contains("json")
        || type.contains("xml")
        || type.contains("x-www-form-urlencoded")
        || type.contains("graphql");
  }

  private static Charset charset(String encoding) {
    try {
      return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    } catch (IllegalArgumentException e) {
      return StandardCharsets.UTF_8;
    }
  }
}
