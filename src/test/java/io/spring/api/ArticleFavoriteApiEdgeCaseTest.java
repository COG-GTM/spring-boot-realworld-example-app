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
public class ArticleFavoriteApiEdgeCaseTest extends TestWithCurrentUser {

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
  public void should_get_401_when_favoriting_without_auth() {
    given().when().post("/articles/{slug}/favorite", "some-article").then().statusCode(401);
  }

  @Test
  public void should_get_401_when_unfavoriting_without_auth() {
    given().when().delete("/articles/{slug}/favorite", "some-article").then().statusCode(401);
  }

  @Test
  public void should_get_404_when_favoriting_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/favorite", "non-existent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_unfavoriting_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/favorite", "non-existent")
        .then()
        .statusCode(404);
  }
}
