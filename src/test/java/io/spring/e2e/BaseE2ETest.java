package io.spring.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseE2ETest {

  @LocalServerPort protected int port;

  protected WebDriver driver;
  protected RestTemplate restTemplate;
  protected ObjectMapper objectMapper;

  @BeforeEach
  public void baseSetUp() {
    driver = new HtmlUnitDriver(true);
    restTemplate = new RestTemplate();
    objectMapper = new ObjectMapper();
  }

  protected String baseUrl() {
    return "http://localhost:" + port;
  }

  protected HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  protected HttpHeaders authHeaders(String token) {
    HttpHeaders headers = jsonHeaders();
    headers.set("Authorization", "Token " + token);
    return headers;
  }

  protected ResponseEntity<String> post(String path, Object body) {
    HttpEntity<String> entity = new HttpEntity<>(toJson(body), jsonHeaders());
    return restTemplate.exchange(baseUrl() + path, HttpMethod.POST, entity, String.class);
  }

  protected ResponseEntity<String> post(String path, Object body, String token) {
    HttpEntity<String> entity = new HttpEntity<>(toJson(body), authHeaders(token));
    return restTemplate.exchange(baseUrl() + path, HttpMethod.POST, entity, String.class);
  }

  protected ResponseEntity<String> get(String path) {
    HttpEntity<String> entity = new HttpEntity<>(jsonHeaders());
    return restTemplate.exchange(baseUrl() + path, HttpMethod.GET, entity, String.class);
  }

  protected ResponseEntity<String> get(String path, String token) {
    HttpEntity<String> entity = new HttpEntity<>(authHeaders(token));
    return restTemplate.exchange(baseUrl() + path, HttpMethod.GET, entity, String.class);
  }

  protected ResponseEntity<String> put(String path, Object body, String token) {
    HttpEntity<String> entity = new HttpEntity<>(toJson(body), authHeaders(token));
    return restTemplate.exchange(baseUrl() + path, HttpMethod.PUT, entity, String.class);
  }

  protected ResponseEntity<String> delete(String path, String token) {
    HttpEntity<String> entity = new HttpEntity<>(authHeaders(token));
    return restTemplate.exchange(baseUrl() + path, HttpMethod.DELETE, entity, String.class);
  }

  protected String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize object to JSON", e);
    }
  }

  protected JsonNode parseJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON response", e);
    }
  }

  /**
   * Register a new user and return the auth token.
   *
   * @param email user email
   * @param username user username
   * @param password user password
   * @return JWT token string
   */
  protected String registerAndGetToken(String email, String username, String password) {
    Map<String, Object> param = userParam(email, username, password);
    ResponseEntity<String> response = post("/users", param);
    JsonNode body = parseJson(response.getBody());
    return body.get("user").get("token").asText();
  }

  /**
   * Build a user registration request body.
   *
   * @param email user email
   * @param username user username
   * @param password user password
   * @return request body map
   */
  protected Map<String, Object> userParam(String email, String username, String password) {
    Map<String, Object> user = new HashMap<>();
    user.put("email", email);
    user.put("username", username);
    user.put("password", password);
    Map<String, Object> param = new HashMap<>();
    param.put("user", user);
    return param;
  }

  /**
   * Build a login request body.
   *
   * @param email user email
   * @param password user password
   * @return request body map
   */
  protected Map<String, Object> loginParam(String email, String password) {
    Map<String, Object> user = new HashMap<>();
    user.put("email", email);
    user.put("password", password);
    Map<String, Object> param = new HashMap<>();
    param.put("user", user);
    return param;
  }

  /**
   * Build an article creation request body.
   *
   * @param title article title
   * @param description article description
   * @param body article body
   * @param tagList list of tags
   * @return request body map
   */
  protected Map<String, Object> articleParam(
      String title, String description, String body, String[] tagList) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", title);
    article.put("description", description);
    article.put("body", body);
    if (tagList != null) {
      article.put("tagList", tagList);
    }
    Map<String, Object> param = new HashMap<>();
    param.put("article", article);
    return param;
  }

  /**
   * Build a comment creation request body.
   *
   * @param body comment text
   * @return request body map
   */
  protected Map<String, Object> commentParam(String body) {
    Map<String, Object> comment = new HashMap<>();
    comment.put("body", body);
    Map<String, Object> param = new HashMap<>();
    param.put("comment", comment);
    return param;
  }

  /**
   * Navigate to a URL using Selenium WebDriver and return the page source.
   *
   * @param path the URL path to navigate to
   * @return page source as string
   */
  protected String navigateAndGetSource(String path) {
    driver.get(baseUrl() + path);
    return driver.getPageSource();
  }

  /**
   * Attempt an HTTP call that is expected to fail with a client error.
   *
   * @param method HTTP method
   * @param path URL path
   * @param body request body (can be null)
   * @param token auth token (can be null)
   * @return the HttpClientErrorException thrown
   */
  protected HttpClientErrorException expectClientError(
      HttpMethod method, String path, Object body, String token) {
    try {
      HttpHeaders headers = token != null ? authHeaders(token) : jsonHeaders();
      HttpEntity<String> entity =
          body != null ? new HttpEntity<>(toJson(body), headers) : new HttpEntity<>(headers);
      restTemplate.exchange(baseUrl() + path, method, entity, String.class);
      throw new AssertionError("Expected HttpClientErrorException but request succeeded");
    } catch (HttpClientErrorException e) {
      return e;
    }
  }
}
