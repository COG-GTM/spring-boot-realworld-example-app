package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.application.ArticleQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLHttpConfigTest {
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private TagsQueryService tagsQueryService;
  @MockitoBean private ArticleQueryService articleQueryService;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private UserQueryService userQueryService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode post(String path, String body) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> response =
        restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    assertEquals(200, response.getStatusCode().value(), response.getBody());
    return objectMapper.readTree(response.getBody());
  }

  @Test
  public void should_accept_unwrapped_graphql_request_body() throws Exception {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    JsonNode json = post("/graphql", "{\"query\":\"{ tags }\"}");

    assertNull(json.get("errors"));
    assertEquals("java", json.at("/data/tags/0").asText());
    assertEquals("spring", json.at("/data/tags/1").asText());
  }

  @Test
  public void should_bind_graphql_variables() throws Exception {
    when(articleQueryService.findBySlug(eq("my-slug"), any())).thenReturn(Optional.empty());

    JsonNode json =
        post(
            "/graphql",
            "{\"query\":\"query($slug: String!) { article(slug: $slug) { slug } }\","
                + "\"variables\":{\"slug\":\"my-slug\"}}");

    verify(articleQueryService).findBySlug(eq("my-slug"), any());
    assertTrue(json.at("/data/article").isNull());
    assertEquals("article", json.at("/errors/0/path/0").asText());
  }

  @Test
  public void should_keep_unwrapping_rest_request_body() throws Exception {
    User user = new User("john@jacob.com", "johnjacob", passwordEncoder.encode("123"), "", "");
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(Optional.of(new UserData(user.getId(), "john@jacob.com", "johnjacob", "", "")));

    JsonNode json =
        post("/users/login", "{\"user\":{\"email\":\"john@jacob.com\",\"password\":\"123\"}}");

    assertEquals("john@jacob.com", json.at("/user/email").asText());
    assertEquals("johnjacob", json.at("/user/username").asText());
  }
}
