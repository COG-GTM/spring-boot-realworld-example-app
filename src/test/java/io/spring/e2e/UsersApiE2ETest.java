package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class UsersApiE2ETest {

  @LocalServerPort private int port;

  private WebDriver driver;
  private RestTemplate restTemplate;
  private ObjectMapper objectMapper;
  private String baseUrl;

  private String testEmail;
  private String testUsername;
  private String testPassword;
  private String authToken;

  @BeforeAll
  public void setUp() {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-gpu");
    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

    restTemplate = new RestTemplate();
    objectMapper = new ObjectMapper();
    baseUrl = "http://localhost:" + port;

    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    testEmail = "testuser_" + uniqueId + "@example.com";
    testUsername = "testuser_" + uniqueId;
    testPassword = "password123";
  }

  @AfterAll
  public void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  @Order(1)
  public void testUserRegistration_Success() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", testEmail);
    userMap.put("username", testUsername);
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/users", request, String.class);

    assertEquals(201, response.getStatusCodeValue());

    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    JsonNode userNode = jsonResponse.get("user");

    assertNotNull(userNode);
    assertEquals(testEmail, userNode.get("email").asText());
    assertEquals(testUsername, userNode.get("username").asText());
    assertNotNull(userNode.get("token").asText());
    assertFalse(userNode.get("token").asText().isEmpty());

    authToken = userNode.get("token").asText();

    driver.get(baseUrl + "/tags");
    String pageSource = driver.getPageSource();
    assertTrue(pageSource.contains("tags") || pageSource.contains("[]"));
  }

  @Test
  @Order(2)
  public void testUserRegistration_DuplicateEmail() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", testEmail);
    userMap.put("username", "different_" + testUsername);
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users", request, String.class);
      fail("Expected HttpClientErrorException for duplicate email");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
      assertTrue(e.getResponseBodyAsString().contains("email"));
    }
  }

  @Test
  @Order(3)
  public void testUserRegistration_DuplicateUsername() throws Exception {
    String uniqueEmail = "unique_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", uniqueEmail);
    userMap.put("username", testUsername);
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users", request, String.class);
      fail("Expected HttpClientErrorException for duplicate username");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
      assertTrue(e.getResponseBodyAsString().contains("username"));
    }
  }

  @Test
  @Order(4)
  public void testUserRegistration_InvalidEmail() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", "invalid-email");
    userMap.put("username", "newuser_" + UUID.randomUUID().toString().substring(0, 8));
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users", request, String.class);
      fail("Expected HttpClientErrorException for invalid email");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
      assertTrue(e.getResponseBodyAsString().contains("email"));
    }
  }

  @Test
  @Order(5)
  public void testUserRegistration_BlankUsername() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", "blank_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
    userMap.put("username", "");
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users", request, String.class);
      fail("Expected HttpClientErrorException for blank username");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
      assertTrue(e.getResponseBodyAsString().contains("username"));
    }
  }

  @Test
  @Order(6)
  public void testUserLogin_Success() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", testEmail);
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/users/login", request, String.class);

    assertEquals(200, response.getStatusCodeValue());

    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    JsonNode userNode = jsonResponse.get("user");

    assertNotNull(userNode);
    assertEquals(testEmail, userNode.get("email").asText());
    assertEquals(testUsername, userNode.get("username").asText());
    assertNotNull(userNode.get("token").asText());
    assertFalse(userNode.get("token").asText().isEmpty());

    driver.get(baseUrl + "/tags");
    WebDriverWait wait = new WebDriverWait(driver, 5);
    String pageSource = driver.getPageSource();
    assertNotNull(pageSource);
  }

  @Test
  @Order(7)
  public void testUserLogin_WrongPassword() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", testEmail);
    userMap.put("password", "wrongpassword");

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users/login", request, String.class);
      fail("Expected HttpClientErrorException for wrong password");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
      assertTrue(e.getResponseBodyAsString().contains("invalid"));
    }
  }

  @Test
  @Order(8)
  public void testUserLogin_NonExistentUser() throws Exception {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("email", "nonexistent@example.com");
    userMap.put("password", testPassword);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(baseUrl + "/users/login", request, String.class);
      fail("Expected HttpClientErrorException for non-existent user");
    } catch (HttpClientErrorException e) {
      assertEquals(422, e.getRawStatusCode());
    }
  }

  @Test
  @Order(9)
  public void testGetCurrentUser_WithValidToken() throws Exception {
    assertNotNull(authToken, "Auth token should be set from registration test");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + authToken);

    HttpEntity<String> request = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/user", HttpMethod.GET, request, String.class);

    assertEquals(200, response.getStatusCodeValue());

    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    JsonNode userNode = jsonResponse.get("user");

    assertNotNull(userNode);
    assertEquals(testEmail, userNode.get("email").asText());
    assertEquals(testUsername, userNode.get("username").asText());
  }

  @Test
  @Order(10)
  public void testGetCurrentUser_WithoutToken() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> request = new HttpEntity<>(headers);

    try {
      restTemplate.exchange(baseUrl + "/user", HttpMethod.GET, request, String.class);
      fail("Expected HttpClientErrorException for missing token");
    } catch (HttpClientErrorException e) {
      assertEquals(401, e.getRawStatusCode());
    }
  }

  @Test
  @Order(11)
  public void testUpdateUser_Success() throws Exception {
    assertNotNull(authToken, "Auth token should be set from registration test");

    String newBio = "Updated bio for E2E test";

    Map<String, Object> userMap = new HashMap<>();
    userMap.put("bio", newBio);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("user", userMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + authToken);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/user", HttpMethod.PUT, request, String.class);

    assertEquals(200, response.getStatusCodeValue());

    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    JsonNode userNode = jsonResponse.get("user");

    assertNotNull(userNode);
    assertEquals(newBio, userNode.get("bio").asText());
    assertEquals(testEmail, userNode.get("email").asText());
    assertEquals(testUsername, userNode.get("username").asText());
  }

  @Test
  @Order(12)
  public void testSeleniumBrowserInteraction_VerifyApiEndpoint() throws Exception {
    driver.get(baseUrl + "/tags");

    String pageSource = driver.getPageSource();
    assertNotNull(pageSource);

    assertTrue(
        pageSource.contains("tags") || pageSource.contains("pre") || pageSource.contains("{"),
        "Page should contain JSON response or HTML wrapper");

    JavascriptExecutor js = (JavascriptExecutor) driver;
    Object result =
        js.executeScript(
            "return fetch('"
                + baseUrl
                + "/tags').then(r => r.json()).then(d => JSON.stringify(d));");

    if (result != null) {
      String jsonResult = result.toString();
      assertTrue(jsonResult.contains("tags"), "Response should contain tags field");
    }
  }
}
