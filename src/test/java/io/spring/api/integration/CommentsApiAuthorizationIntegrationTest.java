package io.spring.api.integration;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end authorization checks for {@code /articles/{slug}/comments} running against the full
 * application context: real security filter chain, real JWT signing/verification and a real SQLite
 * database migrated by Flyway. Each test runs in a transaction that is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CommentsApiAuthorizationIntegrationTest {

  private static final Path DB_FILE = createTempDbFile();

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_FILE.toAbsolutePath());
  }

  @AfterAll
  static void deleteDb() throws IOException {
    Files.deleteIfExists(DB_FILE);
  }

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private CommentReadService commentReadService;
  @Autowired private JwtService jwtService;

  private User articleAuthor;
  private User commentAuthor;
  private User stranger;
  private Article article;
  private Comment comment;

  @BeforeEach
  void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);

    articleAuthor = saveUser("article-author");
    commentAuthor = saveUser("comment-author");
    stranger = saveUser("stranger");

    article = saveArticle("Integration article", articleAuthor);
    comment = new Comment("a comment", commentAuthor.getId(), article.getId());
    commentRepository.save(comment);
  }

  @Nested
  class MissingArticle {

    @Test
    void create_comment_on_missing_article_returns_404_and_persists_nothing() {
      String missingSlug = "no-such-article-" + UUID.randomUUID();

      given()
          .contentType("application/json")
          .header("Authorization", tokenFor(commentAuthor))
          .body(commentBody("hello"))
          .when()
          .post("/articles/{slug}/comments", missingSlug)
          .then()
          .statusCode(404);

      assertEquals(1, commentReadService.findByArticleId(article.getId()).size());
    }

    @Test
    void list_comments_of_missing_article_returns_404() {
      given()
          .when()
          .get("/articles/{slug}/comments", "no-such-article-" + UUID.randomUUID())
          .then()
          .statusCode(404);
    }

    @Test
    void delete_comment_on_missing_article_returns_404_and_keeps_comment() {
      given()
          .header("Authorization", tokenFor(commentAuthor))
          .when()
          .delete(
              "/articles/{slug}/comments/{id}",
              "no-such-article-" + UUID.randomUUID(),
              comment.getId())
          .then()
          .statusCode(404);

      assertCommentExists(comment);
    }

    @Test
    void delete_missing_comment_on_existing_article_returns_404() {
      given()
          .header("Authorization", tokenFor(articleAuthor))
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), UUID.randomUUID().toString())
          .then()
          .statusCode(404);

      assertCommentExists(comment);
    }

    @Test
    void delete_comment_through_a_different_article_slug_returns_404_and_keeps_comment() {
      Article strangersArticle = saveArticle("Stranger article", stranger);

      given()
          .header("Authorization", tokenFor(stranger))
          .when()
          .delete("/articles/{slug}/comments/{id}", strangersArticle.getSlug(), comment.getId())
          .then()
          .statusCode(404);

      assertCommentExists(comment);
    }
  }

  @Nested
  class DeletingSomeoneElsesComment {

    @Test
    void user_who_owns_neither_article_nor_comment_gets_403_and_comment_is_kept() {
      given()
          .header("Authorization", tokenFor(stranger))
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
          .then()
          .statusCode(403);

      assertCommentExists(comment);
    }

    @Test
    void comment_author_on_someone_elses_article_cannot_delete_other_users_comment() {
      Comment strangersComment = new Comment("stranger says hi", stranger.getId(), article.getId());
      commentRepository.save(strangersComment);

      given()
          .header("Authorization", tokenFor(commentAuthor))
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), strangersComment.getId())
          .then()
          .statusCode(403);

      assertCommentExists(strangersComment);
    }

    @Test
    void comment_author_can_delete_own_comment() {
      given()
          .header("Authorization", tokenFor(commentAuthor))
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
          .then()
          .statusCode(204);

      assertCommentDeleted(comment);
    }

    @Test
    void article_author_can_delete_any_comment_on_own_article() {
      given()
          .header("Authorization", tokenFor(articleAuthor))
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
          .then()
          .statusCode(204);

      assertCommentDeleted(comment);
    }
  }

  @Nested
  class UnauthenticatedAccess {

    @Test
    void create_comment_without_token_returns_401_and_persists_nothing() {
      given()
          .contentType("application/json")
          .body(commentBody("anonymous"))
          .when()
          .post("/articles/{slug}/comments", article.getSlug())
          .then()
          .statusCode(401);

      assertEquals(1, commentReadService.findByArticleId(article.getId()).size());
    }

    @Test
    void create_comment_on_missing_article_without_token_returns_401_not_404() {
      given()
          .contentType("application/json")
          .body(commentBody("anonymous"))
          .when()
          .post("/articles/{slug}/comments", "no-such-article-" + UUID.randomUUID())
          .then()
          .statusCode(401);
    }

    @Test
    void delete_comment_without_token_returns_401_and_keeps_comment() {
      given()
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
          .then()
          .statusCode(401);

      assertCommentExists(comment);
    }

    @Test
    void delete_comment_with_tampered_token_returns_401_and_keeps_comment() {
      String token = jwtService.toToken(commentAuthor);
      String tampered = token.substring(0, token.length() - 2) + "xx";

      given()
          .header("Authorization", "Token " + tampered)
          .when()
          .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
          .then()
          .statusCode(401);

      assertCommentExists(comment);
    }

    @Test
    void create_comment_with_malformed_authorization_header_returns_401() {
      given()
          .contentType("application/json")
          .header("Authorization", "Token")
          .body(commentBody("no token value"))
          .when()
          .post("/articles/{slug}/comments", article.getSlug())
          .then()
          .statusCode(401);

      assertEquals(1, commentReadService.findByArticleId(article.getId()).size());
    }

    @Test
    void create_comment_with_token_for_unknown_user_returns_401() {
      User ghost = new User("ghost@example.com", "ghost", "pw", "", "");

      given()
          .contentType("application/json")
          .header("Authorization", tokenFor(ghost))
          .body(commentBody("boo"))
          .when()
          .post("/articles/{slug}/comments", article.getSlug())
          .then()
          .statusCode(401);

      assertEquals(1, commentReadService.findByArticleId(article.getId()).size());
    }

    @Test
    void list_comments_without_token_is_allowed() {
      given()
          .when()
          .get("/articles/{slug}/comments", article.getSlug())
          .then()
          .statusCode(200)
          .body("comments", hasSize(1))
          .body("comments[0].id", equalTo(comment.getId()))
          .body("comments[0].author.following", equalTo(false));
    }
  }

  private User saveUser(String prefix) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    User user =
        new User(prefix + "-" + suffix + "@example.com", prefix + "-" + suffix, "pw", "", "");
    userRepository.save(user);
    return user;
  }

  private Article saveArticle(String title, User author) {
    Article saved =
        new Article(
            title + " " + UUID.randomUUID(),
            "desc",
            "body",
            Collections.singletonList("integration"),
            author.getId());
    articleRepository.save(saved);
    return saved;
  }

  private String tokenFor(User user) {
    return "Token " + jwtService.toToken(user);
  }

  private void assertCommentExists(Comment expected) {
    assertTrue(
        commentRepository.findById(expected.getArticleId(), expected.getId()).isPresent(),
        "comment should still exist");
  }

  private void assertCommentDeleted(Comment expected) {
    assertFalse(
        commentRepository.findById(expected.getArticleId(), expected.getId()).isPresent(),
        "comment should have been deleted");
  }

  private static Map<String, Object> commentBody(String body) {
    Map<String, Object> inner = new HashMap<>();
    inner.put("body", body);
    Map<String, Object> outer = new HashMap<>();
    outer.put("comment", inner);
    return outer;
  }

  private static Path createTempDbFile() {
    try {
      Path file = Files.createTempFile("comments-api-it-", ".db");
      Files.delete(file);
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
