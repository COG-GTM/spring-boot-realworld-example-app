package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleFavoriteApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleFavoriteApiErrorTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_return_404_when_article_not_found_for_favorite() throws Exception {
    when(articleRepository.findBySlug(eq("nonexistent-slug"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/favorite", "nonexistent-slug")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_return_404_when_article_not_found_for_unfavorite() throws Exception {
    when(articleRepository.findBySlug(eq("nonexistent-slug"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/favorite", "nonexistent-slug")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_return_401_when_favoriting_without_auth() throws Exception {
    given().when().post("/articles/{slug}/favorite", "some-slug").then().statusCode(401);
  }

  @Test
  public void should_return_401_when_unfavoriting_without_auth() throws Exception {
    given().when().delete("/articles/{slug}/favorite", "some-slug").then().statusCode(401);
  }
}
