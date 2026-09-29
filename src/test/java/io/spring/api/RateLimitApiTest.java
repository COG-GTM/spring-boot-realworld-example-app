package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticlesApi.class, CommentsApi.class, ProfileApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
@TestPropertySource(
    properties = {
      "rate-limit.enabled=true",
      "rate-limit.limits.article-create.requests=2",
      "rate-limit.limits.article-create.window=1m",
      "rate-limit.limits.comment-create.requests=2",
      "rate-limit.limits.comment-create.window=30s",
      "rate-limit.limits.follow.requests=1",
      "rate-limit.limits.follow.window=10s"
    })
public class RateLimitApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ArticleCommandService articleCommandService;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ProfileQueryService profileQueryService;

  private Article article;
  private User anotherUser;
  private String anotherToken;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);

    article = new Article("title", "desc", "body", asList("java"), user.getId());
    ProfileData author =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            new DateTime(),
            new DateTime(),
            article.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()),
            author);
    when(articleQueryService.findBySlug(anyString(), any())).thenReturn(Optional.empty());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), any()))
        .thenReturn(
            Optional.of(
                new CommentData(
                    "comment-id",
                    "body",
                    article.getId(),
                    new DateTime(),
                    new DateTime(),
                    author)));

    anotherUser = new User("other@test.com", "other", "123", "", "");
    anotherToken = "other-token";
    when(userRepository.findByUsername(eq(anotherUser.getUsername())))
        .thenReturn(Optional.of(anotherUser));
    when(userRepository.findById(eq(anotherUser.getId()))).thenReturn(Optional.of(anotherUser));
    when(jwtService.getSubFromToken(eq(anotherToken))).thenReturn(Optional.of(anotherUser.getId()));
    when(profileQueryService.findByUsername(anyString(), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(anotherUser.getId(), anotherUser.getUsername(), "", "", true)));
  }

  @Test
  public void should_return_429_after_article_create_limit_is_exceeded() {
    createArticle(token)
        .statusCode(200)
        .header("X-RateLimit-Limit", "2")
        .header("X-RateLimit-Remaining", "1");
    createArticle(token).statusCode(200).header("X-RateLimit-Remaining", "0");

    createArticle(token)
        .statusCode(429)
        .header("Retry-After", "60")
        .header("X-RateLimit-Limit", "2")
        .header("X-RateLimit-Remaining", "0")
        .body("errors.rate_limit[0]", containsString("too many requests"));

    verify(articleCommandService, times(2)).createArticle(any(), any());
  }

  @Test
  public void should_return_429_after_comment_create_limit_is_exceeded() {
    createComment(token).statusCode(201);
    createComment(token).statusCode(201);

    createComment(token)
        .statusCode(429)
        .header("Retry-After", "30")
        .body("errors.rate_limit[0]", containsString("retry after 30 seconds"));

    verify(commentRepository, times(2)).save(any());
  }

  @Test
  public void should_return_429_after_follow_limit_is_exceeded() {
    follow(token, anotherUser.getUsername()).statusCode(200);

    follow(token, anotherUser.getUsername()).statusCode(429).header("Retry-After", "10");

    verify(userRepository, times(1))
        .saveRelation(new FollowRelation(user.getId(), anotherUser.getId()));
  }

  @Test
  public void should_limit_each_user_independently() {
    createArticle(token).statusCode(200);
    createArticle(token).statusCode(200);
    createArticle(token).statusCode(429);

    createArticle(anotherToken).statusCode(200).header("X-RateLimit-Remaining", "1");
  }

  @Test
  public void should_limit_each_action_independently() {
    follow(token, anotherUser.getUsername()).statusCode(200);
    follow(token, anotherUser.getUsername()).statusCode(429);

    createArticle(token).statusCode(200);
    createComment(token).statusCode(201);
  }

  @Test
  public void should_not_limit_read_or_unannotated_write_endpoints() {
    for (int i = 0; i < 5; i++) {
      given()
          .when()
          .get("/articles/{slug}/comments", article.getSlug())
          .then()
          .statusCode(200)
          .header("X-RateLimit-Limit", nullValue());
    }
    when(userRepository.findRelation(eq(user.getId()), eq(anotherUser.getId())))
        .thenReturn(Optional.of(new FollowRelation(user.getId(), anotherUser.getId())));
    for (int i = 0; i < 3; i++) {
      given()
          .header("Authorization", "Token " + token)
          .when()
          .delete("/profiles/{username}/follow", anotherUser.getUsername())
          .then()
          .statusCode(200);
    }
  }

  @Test
  public void should_reject_unauthenticated_request_before_counting() {
    for (int i = 0; i < 3; i++) {
      given()
          .contentType("application/json")
          .body(articleParam())
          .when()
          .post("/articles")
          .then()
          .statusCode(401);
    }
    createArticle(token).statusCode(200).header("X-RateLimit-Remaining", "1");
  }

  @Test
  public void should_keep_existing_behavior_for_allowed_requests() {
    createArticle(token).statusCode(200).body("article.title", equalTo(article.getTitle()));
  }

  private ValidatableMockMvcResponse createArticle(String authToken) {
    return given()
        .contentType("application/json")
        .header("Authorization", "Token " + authToken)
        .body(articleParam())
        .when()
        .post("/articles")
        .then();
  }

  private ValidatableMockMvcResponse createComment(String authToken) {
    Map<String, Object> param = new HashMap<>();
    param.put("comment", Collections.singletonMap("body", "comment content"));
    return given()
        .contentType("application/json")
        .header("Authorization", "Token " + authToken)
        .body(param)
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then();
  }

  private ValidatableMockMvcResponse follow(String authToken, String username) {
    return given()
        .header("Authorization", "Token " + authToken)
        .when()
        .post("/profiles/{username}/follow", username)
        .then();
  }

  private Map<String, Object> articleParam() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("title", "How to train your dragon");
    fields.put("description", "Ever wonder how?");
    fields.put("body", "You have to believe");
    fields.put("tagList", asList("dragons"));
    Map<String, Object> param = new HashMap<>();
    param.put("article", fields);
    return param;
  }
}
