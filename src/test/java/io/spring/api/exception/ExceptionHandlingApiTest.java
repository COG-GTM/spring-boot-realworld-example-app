package io.spring.api.exception;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.CommentsApi;
import io.spring.api.ProfileApi;
import io.spring.api.UsersApi;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.user.UserService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End to end coverage of the {@code @ControllerAdvice} wiring: the exceptions are raised by the
 * real controllers and translated by {@link CustomizeExceptionHandler}.
 */
@WebMvcTest({UsersApi.class, CommentsApi.class, ProfileApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ExceptionHandlingApiTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;
  @MockBean private JwtService jwtService;
  @MockBean private UserQueryService userQueryService;
  @MockBean private UserService userService;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ProfileQueryService profileQueryService;

  private User user;
  private String token;
  private Article article;
  private Comment comment;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);

    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));

    User anotherUser = new User("other@example.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), anotherUser.getId());
    comment = new Comment("comment", anotherUser.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));
  }

  @Test
  public void should_return_422_with_all_violations_of_the_same_field() {
    Map<String, Object> param = loginParam(" ", "");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422)
        .body("errors", aMapWithSize(2))
        .body("errors.email", hasSize(2))
        .body("errors.email", containsInAnyOrder("can't be empty", "should be an email"))
        .body("errors.password", contains("can't be empty"));
  }

  @Test
  public void should_return_422_with_one_message_per_violated_field() {
    Map<String, Object> param = loginParam("not-an-email", "123");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/users/login")
        .then()
        .statusCode(422)
        .body("errors", aMapWithSize(1))
        .body("errors.email", contains("should be an email"));
  }

  @Test
  public void should_return_404_when_the_article_of_the_comments_does_not_exist() {
    when(articleRepository.findBySlug(eq("not-exists"))).thenReturn(Optional.empty());

    RestAssuredMockMvc.when().get("/articles/{slug}/comments", "not-exists").then().statusCode(404);
  }

  @Test
  public void should_return_404_when_deleting_a_comment_that_does_not_exist() {
    when(commentRepository.findById(eq(article.getId()), eq("not-exists")))
        .thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), "not-exists")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_return_404_when_unfollowing_an_unknown_user() {
    when(userRepository.findByUsername(eq("unknown"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", "unknown")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_return_403_when_deleting_a_comment_of_another_user() {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(403);

    verify(commentRepository, never()).remove(eq(comment));
  }

  private Map<String, Object> loginParam(final String email, final String password) {
    return new HashMap<String, Object>() {
      {
        put(
            "user",
            new HashMap<String, Object>() {
              {
                put("email", email);
                put("password", password);
              }
            });
      }
    };
  }
}
