package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.service.DefaultJwtService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties =
        "spring.datasource.url=jdbc:sqlite:file:comments_api_authz_it?mode=memory&cache=shared")
public class CommentsApiAuthorizationIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private JwtService jwtService;

  @Value("${jwt.secret}")
  private String jwtSecret;

  private User articleAuthor;
  private User commentAuthor;
  private User stranger;
  private Article article;
  private Comment comment;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);

    articleAuthor = saveUser("article-author");
    commentAuthor = saveUser("comment-author");
    stranger = saveUser("stranger");

    article = saveArticle(articleAuthor);
    comment = new Comment("a comment", commentAuthor.getId(), article.getId());
    commentRepository.save(comment);
  }

  // --- commenting on a missing article ---

  @Test
  public void should_get_404_when_creating_comment_on_missing_article() {
    given()
        .contentType("application/json")
        .header("Authorization", tokenOf(commentAuthor))
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", missingSlug())
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_listing_comments_of_missing_article() {
    RestAssuredMockMvc.when()
        .get("/articles/{slug}/comments", missingSlug())
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_deleting_comment_on_missing_article() {
    given()
        .header("Authorization", tokenOf(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", missingSlug(), comment.getId())
        .then()
        .statusCode(404);

    assertCommentExists();
  }

  @Test
  public void should_get_404_when_deleting_missing_comment_on_existing_article() {
    given()
        .header("Authorization", tokenOf(articleAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), UUID.randomUUID().toString())
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_deleting_comment_through_a_different_article() {
    Article otherArticle = saveArticle(commentAuthor);

    given()
        .header("Authorization", tokenOf(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", otherArticle.getSlug(), comment.getId())
        .then()
        .statusCode(404);

    assertCommentExists();
  }

  // --- deleting someone else's comment ---

  @Test
  public void should_get_403_when_deleting_someone_elses_comment() {
    given()
        .header("Authorization", tokenOf(stranger))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(403);

    assertCommentExists();
    RestAssuredMockMvc.when()
        .get("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(200)
        .body("comments.id", hasItem(comment.getId()));
  }

  @Test
  public void should_get_403_when_comment_author_of_another_comment_deletes_it() {
    Comment strangersComment = new Comment("stranger says", stranger.getId(), article.getId());
    commentRepository.save(strangersComment);

    given()
        .header("Authorization", tokenOf(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), strangersComment.getId())
        .then()
        .statusCode(403);

    assertTrue(commentRepository.findById(article.getId(), strangersComment.getId()).isPresent());
  }

  @Test
  public void should_allow_comment_author_to_delete_own_comment() {
    given()
        .header("Authorization", tokenOf(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(204);

    assertCommentDeleted();
  }

  @Test
  public void should_allow_article_author_to_delete_someone_elses_comment() {
    given()
        .header("Authorization", tokenOf(articleAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(204);

    assertCommentDeleted();
    RestAssuredMockMvc.when()
        .get("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(200)
        .body("comments.id", not(hasItem(comment.getId())));
  }

  // --- unauthenticated access ---

  @Test
  public void should_allow_unauthenticated_user_to_list_comments() {
    RestAssuredMockMvc.when()
        .get("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(200)
        .body("comments.id", hasItem(comment.getId()));
  }

  @Test
  public void should_get_401_when_creating_comment_without_token() {
    given()
        .contentType("application/json")
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_not_404_when_creating_comment_on_missing_article_without_token() {
    given()
        .contentType("application/json")
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", missingSlug())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_deleting_comment_without_token() {
    RestAssuredMockMvc.when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(401);

    assertCommentExists();
  }

  @Test
  public void should_get_401_when_creating_comment_with_malformed_authorization_header() {
    given()
        .contentType("application/json")
        .header("Authorization", "Token")
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_creating_comment_with_invalid_token() {
    given()
        .contentType("application/json")
        .header("Authorization", "Token not-a-jwt")
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_deleting_comment_with_token_signed_by_another_key() {
    JwtService foreignJwtService =
        new DefaultJwtService(
            "a-completely-different-signing-secret-that-is-long-enough-for-hs512-signatures", 3600);

    given()
        .header("Authorization", "Token " + foreignJwtService.toToken(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(401);

    assertCommentExists();
  }

  @Test
  public void should_get_401_when_deleting_comment_with_expired_token() {
    JwtService expiredJwtService = new DefaultJwtService(jwtSecret, -60);

    given()
        .header("Authorization", "Token " + expiredJwtService.toToken(commentAuthor))
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(401);

    assertCommentExists();
  }

  @Test
  public void should_get_401_when_creating_comment_with_token_of_unknown_user() {
    User unsavedUser = new User("ghost@example.com", "ghost", "123", "", "");

    given()
        .contentType("application/json")
        .header("Authorization", tokenOf(unsavedUser))
        .body(commentParam("hello"))
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(401);
  }

  private User saveUser(String prefix) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    User user =
        new User(prefix + "-" + suffix + "@example.com", prefix + "-" + suffix, "123", "", "");
    userRepository.save(user);
    return user;
  }

  private Article saveArticle(User author) {
    Article created =
        new Article(
            "article " + UUID.randomUUID(),
            "desc",
            "body",
            Arrays.asList("java", "spring"),
            author.getId());
    articleRepository.save(created);
    return created;
  }

  private String tokenOf(User user) {
    return "Token " + jwtService.toToken(user);
  }

  private static String missingSlug() {
    return "missing-article-" + UUID.randomUUID();
  }

  private static Map<String, Object> commentParam(String body) {
    Map<String, Object> param = new HashMap<>();
    param.put("comment", Collections.singletonMap("body", body));
    return param;
  }

  private void assertCommentExists() {
    assertTrue(commentRepository.findById(article.getId(), comment.getId()).isPresent());
  }

  private void assertCommentDeleted() {
    assertFalse(commentRepository.findById(article.getId(), comment.getId()).isPresent());
  }
}
