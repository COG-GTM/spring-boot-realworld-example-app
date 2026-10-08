package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.spring.TestHelper.articleDataFixture;
import static java.util.Arrays.asList;
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
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Covers docs/specs/article-bookmarks.md AC-5, AC-7, AC-8 for GET /user/bookmarks. */
@WebMvcTest(UserBookmarksApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class UserBookmarksApiTest extends TestWithCurrentUser {
  @MockBean private ArticleQueryService articleQueryService;

  @Autowired private MockMvc mvc;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  private ArticleData bookmarked(String seed) {
    ArticleData data = articleDataFixture(seed, user);
    data.setBookmarked(true);
    return data;
  }

  // AC-5
  @Test
  public void ac5_should_return_401_when_listing_bookmarks_without_token() throws Exception {
    RestAssuredMockMvc.when().get("/user/bookmarks").then().statusCode(401);
    verify(articleQueryService, never()).findUserBookmarks(any(), any());
  }

  // AC-7
  @Test
  public void ac7_should_list_current_user_bookmarks_newest_first() throws Exception {
    when(articleQueryService.findUserBookmarks(eq(user), eq(new Page(0, 20))))
        .thenReturn(new ArticleDataList(asList(bookmarked("2"), bookmarked("1")), 2));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/user/bookmarks")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(2))
        .body("articles.size()", equalTo(2))
        .body("articles[0].slug", equalTo("title-2"))
        .body("articles[1].slug", equalTo("title-1"))
        .body("articles[0].bookmarked", equalTo(true))
        .body("articles[1].bookmarked", equalTo(true));
  }

  // AC-8
  @Test
  public void ac8_should_apply_limit_and_offset_and_return_total_count() throws Exception {
    when(articleQueryService.findUserBookmarks(eq(user), eq(new Page(10, 5))))
        .thenReturn(new ArticleDataList(asList(bookmarked("11")), 11));

    given()
        .header("Authorization", "Token " + token)
        .queryParam("limit", 5)
        .queryParam("offset", 10)
        .when()
        .get("/user/bookmarks")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(11))
        .body("articles.size()", equalTo(1));

    verify(articleQueryService).findUserBookmarks(eq(user), eq(new Page(10, 5)));
  }
}
