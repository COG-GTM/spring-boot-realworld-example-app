package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.TestHelper;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticleApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleApiEdgeCaseTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleCommandService articleCommandService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_read_article_as_authenticated_user() {
    Article article =
        new Article(
            "Test Article", "Desc", "Body", Arrays.asList("java", "spring"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleQueryService.findBySlug(eq(article.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/{slug}", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.slug", equalTo(article.getSlug()))
        .body("article.body", equalTo(articleData.getBody()));
  }

  @Test
  public void should_get_404_when_deleting_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}", "non-existent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_updating_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    HashMap<String, Object> updateParam =
        new HashMap<String, Object>() {
          {
            put(
                "article",
                new HashMap<String, Object>() {
                  {
                    put("title", "new title");
                    put("body", "new body");
                    put("description", "new desc");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(updateParam)
        .when()
        .put("/articles/{slug}", "non-existent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_401_when_updating_article_without_auth() {
    HashMap<String, Object> updateParam =
        new HashMap<String, Object>() {
          {
            put(
                "article",
                new HashMap<String, Object>() {
                  {
                    put("title", "new title");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .body(updateParam)
        .when()
        .put("/articles/{slug}", "some-article")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_deleting_article_without_auth() {
    given().when().delete("/articles/{slug}", "some-article").then().statusCode(401);
  }

  @Test
  public void should_update_article_with_partial_params() {
    Article article =
        new Article(
            "Old Title", "old desc", "old body", Arrays.asList("java"), user.getId());
    Article updatedArticle =
        new Article(
            "New Title", "old desc", "old body", Arrays.asList("java"), user.getId());

    ArticleData updatedArticleData =
        TestHelper.getArticleDataFromArticleAndUser(updatedArticle, user);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);
    when(articleQueryService.findBySlug(eq(updatedArticle.getSlug()), eq(user)))
        .thenReturn(Optional.of(updatedArticleData));

    HashMap<String, Object> updateParam =
        new HashMap<String, Object>() {
          {
            put(
                "article",
                new HashMap<String, Object>() {
                  {
                    put("title", "New Title");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(updateParam)
        .when()
        .put("/articles/{slug}", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.title", equalTo("New Title"));
  }
}
