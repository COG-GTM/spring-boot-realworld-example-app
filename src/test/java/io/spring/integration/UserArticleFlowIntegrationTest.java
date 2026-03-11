package io.spring.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserArticleFlowIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepository userRepository;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private JwtService jwtService;

  @Autowired private PasswordEncoder passwordEncoder;

  private String baseUrl;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    baseUrl = "http://localhost:" + port;
    objectMapper = new ObjectMapper();
  }

  @Test
  public void should_register_user_successfully() throws Exception {
    Map<String, Object> userParam = new HashMap<>();
    Map<String, Object> user = new HashMap<>();
    user.put("email", "test@example.com");
    user.put("username", "testuser");
    user.put("password", "password123");
    userParam.put("user", user);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/users", userParam, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode userData = body.get("user");
    assertThat(userData.get("email").asText(), is("test@example.com"));
    assertThat(userData.get("username").asText(), is("testuser"));
    assertTrue(userData.has("token"));
  }

  @Test
  public void should_login_user_successfully() throws Exception {
    User user =
        new User(
            "login@example.com",
            "loginuser",
            passwordEncoder.encode("password123"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(user);

    Map<String, Object> loginParam = new HashMap<>();
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("email", "login@example.com");
    userLogin.put("password", "password123");
    loginParam.put("user", userLogin);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/users/login", loginParam, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode userData = body.get("user");
    assertThat(userData.get("email").asText(), is("login@example.com"));
    assertTrue(userData.has("token"));
  }

  @Test
  public void should_create_article_when_authenticated() throws Exception {
    User user =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(user);
    String token = jwtService.toToken(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + token);

    Map<String, Object> articleParam = new HashMap<>();
    Map<String, Object> article = new HashMap<>();
    article.put("title", "Test Article");
    article.put("description", "Test Description");
    article.put("body", "Test Body");
    article.put("tagList", Arrays.asList("java", "spring"));
    articleParam.put("article", article);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(articleParam, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/articles", request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode articleData = body.get("article");
    assertThat(articleData.get("title").asText(), is("Test Article"));
    assertThat(articleData.get("slug").asText(), is("test-article"));
  }

  @Test
  public void should_get_article_by_slug() throws Exception {
    User user =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(user);

    Article article =
        new Article(
            "Get Article Test", "Description", "Body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/articles/" + article.getSlug(), String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode articleData = body.get("article");
    assertThat(articleData.get("title").asText(), is("Get Article Test"));
  }

  @Test
  public void should_favorite_article_when_authenticated() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    User reader =
        new User(
            "reader@example.com",
            "reader",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(reader);
    String token = jwtService.toToken(reader);

    Article article =
        new Article("Favorite Test", "Description", "Body", Arrays.asList("java"), author.getId());
    articleRepository.save(article);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Token " + token);

    HttpEntity<Void> request = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            baseUrl + "/articles/" + article.getSlug() + "/favorite", request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode articleData = body.get("article");
    assertThat(articleData.get("favorited").asBoolean(), is(true));
    assertThat(articleData.get("favoritesCount").asInt(), is(1));
  }

  @Test
  public void should_add_comment_to_article() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    User commenter =
        new User(
            "commenter@example.com",
            "commenter",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(commenter);
    String token = jwtService.toToken(commenter);

    Article article =
        new Article("Comment Test", "Description", "Body", Arrays.asList("java"), author.getId());
    articleRepository.save(article);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + token);

    Map<String, Object> commentParam = new HashMap<>();
    Map<String, Object> comment = new HashMap<>();
    comment.put("body", "This is a test comment");
    commentParam.put("comment", comment);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(commentParam, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            baseUrl + "/articles/" + article.getSlug() + "/comments", request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode commentData = body.get("comment");
    assertThat(commentData.get("body").asText(), is("This is a test comment"));
  }

  @Test
  public void should_follow_user_and_see_articles_in_feed()throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    User follower =
        new User(
            "follower@example.com",
            "follower",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(follower);
    String token = jwtService.toToken(follower);

    Article article =
        new Article("Feed Test", "Description", "Body", Arrays.asList("java"), author.getId());
    articleRepository.save(article);

    FollowRelation followRelation = new FollowRelation(follower.getId(), author.getId());
    userRepository.saveRelation(followRelation);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Token " + token);

    HttpEntity<Void> request = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/articles/feed", HttpMethod.GET, request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    assertThat(body.get("articlesCount").asInt(), is(1));
  }

  @Test
  public void should_not_allow_unauthorized_user_to_update_article() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    User otherUser =
        new User(
            "other@example.com",
            "other",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(otherUser);
    String token = jwtService.toToken(otherUser);

    Article article =
        new Article(
            "Unauthorized Update Test",
            "Description",
            "Body",
            Arrays.asList("java"),
            author.getId());
    articleRepository.save(article);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + token);

    Map<String, Object> updateParam = new HashMap<>();
    Map<String, Object> articleUpdate = new HashMap<>();
    articleUpdate.put("title", "Hacked Title");
    updateParam.put("article", articleUpdate);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateParam, headers);
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/articles/" + article.getSlug(), HttpMethod.PUT, request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN));
  }

  @Test
  public void should_allow_author_to_update_own_article() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);
    String token = jwtService.toToken(author);

    Article article =
        new Article(
            "Author Update Test", "Description", "Body", Arrays.asList("java"), author.getId());
    articleRepository.save(article);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + token);

    Map<String, Object> updateParam = new HashMap<>();
    Map<String, Object> articleUpdate = new HashMap<>();
    articleUpdate.put("title", "Updated Title");
    updateParam.put("article", articleUpdate);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateParam, headers);
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/articles/" + article.getSlug(), HttpMethod.PUT, request, String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode articleData = body.get("article");
    assertThat(articleData.get("title").asText(), is("Updated Title"));
  }

  @Test
  public void should_get_user_profile() throws Exception {
    User user =
        new User(
            "profile@example.com",
            "profileuser",
            passwordEncoder.encode("password"),
            "Test bio",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(user);

    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/profiles/profileuser", String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    JsonNode profile = body.get("profile");
    assertThat(profile.get("username").asText(), is("profileuser"));
    assertThat(profile.get("bio").asText(), is("Test bio"));
  }

  @Test
  public void should_list_articles_with_pagination() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    for (int i = 0; i < 5; i++) {
      Article article =
          new Article(
              "Article " + i,
              "Description " + i,
              "Body " + i,
              Arrays.asList("java"),
              author.getId());
      articleRepository.save(article);
    }

    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/articles?limit=3&offset=0", String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    assertThat(body.get("articlesCount").asInt(), is(5));
  }

  @Test
  public void should_filter_articles_by_tag() throws Exception {
    User author =
        new User(
            "author@example.com",
            "author",
            passwordEncoder.encode("password"),
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
    userRepository.save(author);

    Article javaArticle =
        new Article("Java Article", "Description", "Body", Arrays.asList("java"), author.getId());
    articleRepository.save(javaArticle);

    Article springArticle =
        new Article(
            "Spring Article", "Description", "Body", Arrays.asList("spring"), author.getId());
    articleRepository.save(springArticle);

    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/articles?tag=java", String.class);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body, notNullValue());
    assertThat(body.get("articlesCount").asInt(), is(1));
  }
}
