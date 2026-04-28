package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.CommentQueryService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
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

@WebMvcTest(CommentsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class CommentsApiEdgeCaseTest extends TestWithCurrentUser {

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @Autowired private MockMvc mvc;

  private Article article;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    article = new Article("title", "desc", "body", Arrays.asList("test"), user.getId());
  }

  @Test
  public void should_get_404_when_creating_comment_on_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    Map<String, Object> param = prepareCommentParam("my comment");

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(param)
        .when()
        .post("/articles/{slug}/comments", "non-existent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_401_when_creating_comment_without_auth() {
    Map<String, Object> param = prepareCommentParam("my comment");

    given()
        .contentType("application/json")
        .body(param)
        .when()
        .post("/articles/{slug}/comments", article.getSlug())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_deleting_comment_without_auth() {
    given()
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), "commentId")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_404_when_deleting_non_existent_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent")))
        .thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), "non-existent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_allow_article_author_to_delete_others_comment() {
    User commentAuthor = new User("other@test.com", "other", "123", "", "");
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Comment comment = new Comment("content", commentAuthor.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/comments/{id}", article.getSlug(), comment.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void should_get_404_when_getting_comments_of_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    RestAssuredMockMvc.when()
        .get("/articles/{slug}/comments", "non-existent")
        .then()
        .statusCode(404);
  }

  private Map<String, Object> prepareCommentParam(String body) {
    return new HashMap<String, Object>() {
      {
        put(
            "comment",
            new HashMap<String, Object>() {
              {
                put("body", body);
              }
            });
      }
    };
  }
}
