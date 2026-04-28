package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.spring.TestHelper.articleDataFixture;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.ArticleRepository;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticlesApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ListArticleApiFilterTest extends TestWithCurrentUser {

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
  public void should_filter_articles_by_tag() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq("java"), eq(null), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?tag=java")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1));
  }

  @Test
  public void should_filter_articles_by_author() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq(null), eq("johnjacob"), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?author=johnjacob")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1));
  }

  @Test
  public void should_filter_articles_by_favorited() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq("johnjacob"), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?favorited=johnjacob")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1));
  }

  @Test
  public void should_support_pagination_with_offset_and_limit() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 5);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq(null), eq(new Page(2, 5)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?offset=2&limit=5")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(5))
        .body("articles", hasSize(1));
  }

  @Test
  public void should_return_empty_list_when_no_articles() {
    ArticleDataList emptyList = new ArticleDataList(Collections.emptyList(), 0);
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(emptyList);

    RestAssuredMockMvc.when()
        .get("/articles")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(0))
        .body("articles", hasSize(0));
  }

  @Test
  public void should_combine_tag_and_author_filters() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 1);
    when(articleQueryService.findRecentArticles(
            eq("java"), eq("johnjacob"), eq(null), eq(new Page(0, 20)), eq(null)))
        .thenReturn(articleDataList);

    RestAssuredMockMvc.when()
        .get("/articles?tag=java&author=johnjacob")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1));
  }

  @Test
  public void should_get_feed_with_pagination() {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 5);
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(1, 10))))
        .thenReturn(articleDataList);

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed?offset=1&limit=10")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(5));
  }
}
