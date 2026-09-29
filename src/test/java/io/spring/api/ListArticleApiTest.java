package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.spring.TestHelper.articleDataFixture;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.ArticleRepository;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  public void should_get_feeds_with_offset_and_limit() throws Exception {
    ArticleDataList articleDataList =
        new ArticleDataList(asList(articleDataFixture("1", user)), 11);
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(10, 5))))
        .thenReturn(articleDataList);

    given()
        .header("Authorization", "Token " + token)
        .queryParam("offset", 10)
        .queryParam("limit", 5)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(11))
        .body("articles.size()", equalTo(1))
        .body("$", not(hasKey("pageInfo")));
    verify(articleQueryService, never()).findUserFeedWithCursor(any(), any());
  }

  @Test
  public void should_get_first_feed_page_with_empty_cursor() throws Exception {
    ArticleData first = articleDataFixture("1", user);
    ArticleData second = articleDataFixture("2", user);
    second.setCreatedAt(first.getCreatedAt().minusMinutes(1));
    List<ArticleData> articles = asList(first, second);
    when(articleQueryService.findUserFeedWithCursor(
            eq(user), eq(new CursorPageParameter<DateTime>(null, 2, Direction.NEXT))))
        .thenReturn(new CursorPager<>(articles, Direction.NEXT, true));

    given()
        .header("Authorization", "Token " + token)
        .queryParam("cursor", "")
        .queryParam("limit", 2)
        .when()
        .get("/articles/feed")
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("articles.size()", equalTo(2))
        .body("articles[0].slug", equalTo(first.getSlug()))
        .body("$", not(hasKey("articlesCount")))
        .body("pageInfo.startCursor", equalTo(String.valueOf(first.getCreatedAt().getMillis())))
        .body("pageInfo.endCursor", equalTo(String.valueOf(second.getCreatedAt().getMillis())))
        .body("pageInfo.hasNextPage", equalTo(true))
        .body("pageInfo.hasPreviousPage", equalTo(false));
    verify(articleQueryService, never()).findUserFeed(any(), any());
  }

  @Test
  public void should_get_feed_page_before_cursor() throws Exception {
    DateTime cursor = new DateTime().withMillis(1600000000000L);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(new CursorPager<>(asList(articleDataFixture("1", user)), Direction.PREV, true));

    given()
        .header("Authorization", "Token " + token)
        .queryParam("cursor", "1600000000000")
        .queryParam("direction", "prev")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("pageInfo.hasNextPage", equalTo(false))
        .body("pageInfo.hasPreviousPage", equalTo(true));

    ArgumentCaptor<CursorPageParameter> captor = ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    CursorPageParameter<DateTime> page = captor.getValue();
    Assertions.assertEquals(Direction.PREV, page.getDirection());
    Assertions.assertEquals(20, page.getLimit());
    Assertions.assertEquals(cursor.getMillis(), page.getCursor().getMillis());
  }

  @Test
  public void should_reject_invalid_feed_cursor() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .queryParam("cursor", "not-a-cursor")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(422)
        .body("errors.cursor[0]", equalTo("is not a valid cursor"));
  }

  @Test
  public void should_reject_invalid_feed_direction() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .queryParam("direction", "sideways")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(422)
        .body("errors.direction[0]", equalTo("must be 'next' or 'prev'"));
  }

  @Test
  public void should_reject_mixing_offset_and_cursor_on_feed() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .queryParam("offset", 0)
        .queryParam("cursor", "1600000000000")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(422)
        .body("errors.offset[0]", equalTo("can't be combined with cursor or direction"));
  }
}
