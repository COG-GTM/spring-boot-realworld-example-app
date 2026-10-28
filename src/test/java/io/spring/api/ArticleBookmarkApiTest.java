package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.TestHelper;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleBookmarkApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleBookmarkApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleBookmarkRepository articleBookmarkRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    User anotherUser = new User("other@test.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), anotherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    articleData = TestHelper.getArticleDataFromArticleAndUser(article, anotherUser);
    when(articleQueryService.findBySlug(eq(article.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
  }

  @Test
  public void should_bookmark_an_article_success() throws Exception {
    articleData.setBookmarked(true);
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/bookmark", article.getSlug())
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("article.id", equalTo(article.getId()))
        .body("article.bookmarked", equalTo(true))
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0));

    verify(articleBookmarkRepository).save(new ArticleBookmark(article.getId(), user.getId()));
  }

  @Test
  public void should_unbookmark_an_article_success() throws Exception {
    when(articleBookmarkRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(new ArticleBookmark(article.getId(), user.getId())));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", article.getSlug())
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("article.id", equalTo(article.getId()))
        .body("article.bookmarked", equalTo(false));
    verify(articleBookmarkRepository).remove(new ArticleBookmark(article.getId(), user.getId()));
  }

  @Test
  public void should_unbookmark_be_idempotent_when_not_bookmarked() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.bookmarked", equalTo(false));
    verify(articleBookmarkRepository, never()).remove(any());
  }

  @Test
  public void should_get_401_when_bookmark_without_token() throws Exception {
    given().when().post("/articles/{slug}/bookmark", article.getSlug()).then().statusCode(401);
    given().when().delete("/articles/{slug}/bookmark", article.getSlug()).then().statusCode(401);
    verify(articleBookmarkRepository, never()).save(any());
    verify(articleBookmarkRepository, never()).remove(any());
  }

  @Test
  public void should_get_404_when_bookmark_unknown_article() throws Exception {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/bookmark", "unknown-slug")
        .then()
        .statusCode(404);
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", "unknown-slug")
        .then()
        .statusCode(404);
    verify(articleBookmarkRepository, never()).save(any());
  }
}
