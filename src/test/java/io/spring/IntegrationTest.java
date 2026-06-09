package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String baseUrl;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    baseUrl = "http://localhost:" + port;
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders authHeaders(String token) {
    HttpHeaders headers = jsonHeaders();
    headers.set("Authorization", "Token " + token);
    return headers;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseResponse(ResponseEntity<String> response) throws Exception {
    return objectMapper.readValue(response.getBody(), Map.class);
  }

  @SuppressWarnings("unchecked")
  private String registerAndLogin(String email, String username, String password) throws Exception {
    Map<String, Object> userBody = new HashMap<>();
    Map<String, Object> userInner = new HashMap<>();
    userInner.put("email", email);
    userInner.put("username", username);
    userInner.put("password", password);
    userBody.put("user", userInner);

    ResponseEntity<String> registerResponse =
        restTemplate.exchange(
            baseUrl + "/users",
            HttpMethod.POST,
            new HttpEntity<>(userBody, jsonHeaders()),
            String.class);
    assertThat(registerResponse.getStatusCode(), is(HttpStatus.CREATED));

    Map<String, Object> loginBody = new HashMap<>();
    Map<String, Object> loginInner = new HashMap<>();
    loginInner.put("email", email);
    loginInner.put("password", password);
    loginBody.put("user", loginInner);

    ResponseEntity<String> loginResponse =
        restTemplate.exchange(
            baseUrl + "/users/login",
            HttpMethod.POST,
            new HttpEntity<>(loginBody, jsonHeaders()),
            String.class);
    assertThat(loginResponse.getStatusCode(), is(HttpStatus.OK));

    Map<String, Object> loginData = parseResponse(loginResponse);
    Map<String, Object> userData = (Map<String, Object>) loginData.get("user");
    return (String) userData.get("token");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void user_flow_register_login_get_profile_update() throws Exception {
    String token = registerAndLogin("userflow@test.com", "userflowuser", "password123");
    assertThat(token, notNullValue());

    ResponseEntity<String> currentUser =
        restTemplate.exchange(
            baseUrl + "/user",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(currentUser.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> userData = parseResponse(currentUser);
    Map<String, Object> user = (Map<String, Object>) userData.get("user");
    assertThat(user.get("username"), is("userflowuser"));
    assertThat(user.get("email"), is("userflow@test.com"));

    Map<String, Object> updateBody = new HashMap<>();
    Map<String, Object> updateInner = new HashMap<>();
    updateInner.put("bio", "my new bio");
    updateBody.put("user", updateInner);

    ResponseEntity<String> updateResponse =
        restTemplate.exchange(
            baseUrl + "/user",
            HttpMethod.PUT,
            new HttpEntity<>(updateBody, authHeaders(token)),
            String.class);
    assertThat(updateResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> updateData = parseResponse(updateResponse);
    Map<String, Object> updatedUser = (Map<String, Object>) updateData.get("user");
    assertThat(updatedUser.get("bio"), is("my new bio"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void article_flow_create_get_update_list_delete() throws Exception {
    String token = registerAndLogin("articleflow@test.com", "articleflowuser", "password123");

    Map<String, Object> articleBody = new HashMap<>();
    Map<String, Object> articleInner = new HashMap<>();
    articleInner.put("title", "Integration Test Article");
    articleInner.put("description", "test desc");
    articleInner.put("body", "test body");
    articleInner.put("tagList", Arrays.asList("java", "testing"));
    articleBody.put("article", articleInner);

    ResponseEntity<String> createResponse =
        restTemplate.exchange(
            baseUrl + "/articles",
            HttpMethod.POST,
            new HttpEntity<>(articleBody, authHeaders(token)),
            String.class);
    assertThat(createResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> createData = parseResponse(createResponse);
    Map<String, Object> articleData = (Map<String, Object>) createData.get("article");
    String slug = (String) articleData.get("slug");
    assertThat(slug, notNullValue());

    ResponseEntity<String> getResponse =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug,
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            String.class);
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> getData = parseResponse(getResponse);
    Map<String, Object> fetchedArticle = (Map<String, Object>) getData.get("article");
    assertThat(fetchedArticle.get("title"), is("Integration Test Article"));

    Map<String, Object> updateBody = new HashMap<>();
    Map<String, Object> updateInner = new HashMap<>();
    updateInner.put("title", "Updated Integration Title");
    updateBody.put("article", updateInner);

    ResponseEntity<String> updateResponse =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug,
            HttpMethod.PUT,
            new HttpEntity<>(updateBody, authHeaders(token)),
            String.class);
    assertThat(updateResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> updateData = parseResponse(updateResponse);
    Map<String, Object> updatedArticle = (Map<String, Object>) updateData.get("article");
    assertThat(updatedArticle.get("title"), is("Updated Integration Title"));
    String newSlug = (String) updatedArticle.get("slug");

    ResponseEntity<String> listResponse =
        restTemplate.exchange(
            baseUrl + "/articles",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            String.class);
    assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));

    ResponseEntity<String> deleteResponse =
        restTemplate.exchange(
            baseUrl + "/articles/" + newSlug,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(deleteResponse.getStatusCode(), is(HttpStatus.NO_CONTENT));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void comment_flow_create_list_delete() throws Exception {
    String token = registerAndLogin("commentflow@test.com", "commentflowuser", "password123");

    Map<String, Object> articleBody = new HashMap<>();
    Map<String, Object> articleInner = new HashMap<>();
    articleInner.put("title", "Article For Comments");
    articleInner.put("description", "desc");
    articleInner.put("body", "body");
    articleInner.put("tagList", Arrays.asList());
    articleBody.put("article", articleInner);

    ResponseEntity<String> createArticle =
        restTemplate.exchange(
            baseUrl + "/articles",
            HttpMethod.POST,
            new HttpEntity<>(articleBody, authHeaders(token)),
            String.class);
    Map<String, Object> articleResult = parseResponse(createArticle);
    String slug = (String) ((Map<String, Object>) articleResult.get("article")).get("slug");

    Map<String, Object> commentBody = new HashMap<>();
    Map<String, Object> commentInner = new HashMap<>();
    commentInner.put("body", "great article!");
    commentBody.put("comment", commentInner);

    ResponseEntity<String> createComment =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug + "/comments",
            HttpMethod.POST,
            new HttpEntity<>(commentBody, authHeaders(token)),
            String.class);
    assertThat(createComment.getStatusCode(), is(HttpStatus.CREATED));
    Map<String, Object> commentResult = parseResponse(createComment);
    Map<String, Object> commentData = (Map<String, Object>) commentResult.get("comment");
    String commentId = (String) commentData.get("id");
    assertThat(commentId, notNullValue());

    ResponseEntity<String> listComments =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug + "/comments",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(listComments.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> listData = parseResponse(listComments);
    List<?> comments = (List<?>) listData.get("comments");
    assertThat(comments.size(), greaterThanOrEqualTo(1));

    ResponseEntity<String> deleteComment =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug + "/comments/" + commentId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(deleteComment.getStatusCode(), is(HttpStatus.NO_CONTENT));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void favorite_flow_favorite_and_unfavorite() throws Exception {
    String token = registerAndLogin("favflow@test.com", "favflowuser", "password123");

    Map<String, Object> articleBody = new HashMap<>();
    Map<String, Object> articleInner = new HashMap<>();
    articleInner.put("title", "Article For Favorite");
    articleInner.put("description", "desc");
    articleInner.put("body", "body");
    articleInner.put("tagList", Arrays.asList());
    articleBody.put("article", articleInner);

    ResponseEntity<String> createArticle =
        restTemplate.exchange(
            baseUrl + "/articles",
            HttpMethod.POST,
            new HttpEntity<>(articleBody, authHeaders(token)),
            String.class);
    Map<String, Object> createResult = parseResponse(createArticle);
    String slug = (String) ((Map<String, Object>) createResult.get("article")).get("slug");

    ResponseEntity<String> favoriteResponse =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug + "/favorite",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(favoriteResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> favData = parseResponse(favoriteResponse);
    Map<String, Object> favArticle = (Map<String, Object>) favData.get("article");
    assertThat(favArticle.get("favorited"), is(true));
    assertThat(favArticle.get("favoritesCount"), is(1));

    ResponseEntity<String> unfavoriteResponse =
        restTemplate.exchange(
            baseUrl + "/articles/" + slug + "/favorite",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    assertThat(unfavoriteResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> unfavData = parseResponse(unfavoriteResponse);
    Map<String, Object> unfavArticle = (Map<String, Object>) unfavData.get("article");
    assertThat(unfavArticle.get("favorited"), is(false));
    assertThat(unfavArticle.get("favoritesCount"), is(0));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void follow_flow_follow_and_unfollow() throws Exception {
    String token1 = registerAndLogin("followuser1@test.com", "followuser1", "password123");
    String token2 = registerAndLogin("followuser2@test.com", "followuser2", "password123");

    ResponseEntity<String> followResponse =
        restTemplate.exchange(
            baseUrl + "/profiles/followuser2/follow",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders(token1)),
            String.class);
    assertThat(followResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> followData = parseResponse(followResponse);
    Map<String, Object> profile = (Map<String, Object>) followData.get("profile");
    assertThat(profile.get("following"), is(true));
    assertThat(profile.get("username"), is("followuser2"));

    ResponseEntity<String> unfollowResponse =
        restTemplate.exchange(
            baseUrl + "/profiles/followuser2/follow",
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(token1)),
            String.class);
    assertThat(unfollowResponse.getStatusCode(), is(HttpStatus.OK));
    Map<String, Object> unfollowData = parseResponse(unfollowResponse);
    Map<String, Object> unfollowProfile = (Map<String, Object>) unfollowData.get("profile");
    assertThat(unfollowProfile.get("following"), is(false));
  }
}
