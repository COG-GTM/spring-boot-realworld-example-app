package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.spring.TestHelper.articleDataFixture;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticlesApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ListArticleApiTest extends TestWithCurrentUser {
  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @Autowired private MockMvc mvc;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_default_article_list() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(
            asList(articleDataFixture("1", user), articleDataFixture("2", user)), 2);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);
    RestAssuredMockMvc.when().get("/articles").prettyPeek().then().statusCode(200);
  }

  @Test
  public void should_get_feeds_401_without_login() throws Exception {
    RestAssuredMockMvc.when().get("/articles/feed").prettyPeek().then().statusCode(401);
  }

  @Test
  public void should_get_feeds_success() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(
            asList(articleDataFixture("1", user), articleDataFixture("2", user)), 2);
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(0, 20))))
        .thenReturn(articleDataList);

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed")
        .prettyPeek()
        .then()
        .statusCode(200);
  }

  @Test
  public void should_create_article_success() throws Exception {
    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put(
                "article",
                new HashMap<String, Object>() {
                  {
                    put("title", "How to train your dragon");
                    put("description", "Ever wonder how?");
                    put("body", "You have to believe");
                    put("tagList", Arrays.asList("reactjs", "angularjs", "dragons"));
                  }
                });
          }
        };

    Article article =
        new Article(
            "How to train your dragon",
            "Ever wonder how?",
            "You have to believe",
            Arrays.asList("reactjs", "angularjs", "dragons"),
            user.getId());

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(java.util.Optional.of(articleDataFixture("1", user)));

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(param)
        .when()
        .post("/articles")
        .prettyPeek()
        .then()
        .statusCode(200);
  }

  @Test
  public void should_get_422_when_create_article_with_empty_title() throws Exception {
    Map<String, Object> param =
        new HashMap<String, Object>() {
          {
            put(
                "article",
                new HashMap<String, Object>() {
                  {
                    put("title", "");
                    put("description", "");
                    put("body", "");
                  }
                });
          }
        };

    given()
        .contentType("application/json")
        .header("Authorization", "Token " + token)
        .body(param)
        .when()
        .post("/articles")
        .prettyPeek()
        .then()
        .statusCode(422);
  }

  @Test
  public void should_get_articles_with_tag_filter() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq("reactjs"), eq(null), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?tag=reactjs")
        .prettyPeek()
        .then()
        .statusCode(200);
  }

  @Test
  public void should_get_articles_with_author_filter() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq(null), eq("johnjacob"), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?author=johnjacob")
        .prettyPeek()
        .then()
        .statusCode(200);
  }

  @Test
  public void should_get_articles_with_favorited_filter() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq("johnjacob"), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?favorited=johnjacob")
        .prettyPeek()
        .then()
        .statusCode(200);
  }
}
