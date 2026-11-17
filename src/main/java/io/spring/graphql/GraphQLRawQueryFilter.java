package io.spring.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Accepts {@code POST /graphql} with {@code Content-Type: application/graphql} (the raw query as
 * the body), which DGS supported before it moved onto Spring for GraphQL. The body is rewritten as
 * the JSON request {@code {"query": "<body>"}}.
 */
public class GraphQLRawQueryFilter extends OncePerRequestFilter {

  static final MediaType APPLICATION_GRAPHQL = MediaType.parseMediaType("application/graphql");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod()) || request.getContentType() == null) {
      return true;
    }
    try {
      return !APPLICATION_GRAPHQL.includes(MediaType.parseMediaType(request.getContentType()));
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Charset charset =
        request.getCharacterEncoding() != null
            ? Charset.forName(request.getCharacterEncoding())
            : StandardCharsets.UTF_8;
    String query = StreamUtils.copyToString(request.getInputStream(), charset);
    if (query.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("Invalid GraphQL request - no request body was provided");
      return;
    }
    byte[] body = objectMapper.writeValueAsBytes(Map.of("query", query));
    filterChain.doFilter(new JsonQueryRequest(request, body), response);
  }

  private static class JsonQueryRequest extends HttpServletRequestWrapper {
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final byte[] body;

    JsonQueryRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public String getContentType() {
      return JSON;
    }

    @Override
    public String getCharacterEncoding() {
      return StandardCharsets.UTF_8.name();
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public long getContentLengthLong() {
      return body.length;
    }

    @Override
    public String getHeader(String name) {
      if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
        return JSON;
      }
      if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
        return String.valueOf(body.length);
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)
          || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
        return Collections.enumeration(Collections.singletonList(getHeader(name)));
      }
      return super.getHeaders(name);
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream in = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return in.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
