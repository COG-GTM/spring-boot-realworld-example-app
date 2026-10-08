package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.bookmark.ArticleBookmark;
import io.spring.core.bookmark.ArticleBookmarkRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Covers docs/specs/article-bookmarks.md AC-1..AC-6 for the REST bookmark endpoints. */
@WebMvcTest(ArticleBookmarkApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ArticleBookmarkApiTest extends TestWithCurrentUser {
  @Autowired private MockMvc mvc;

  @MockBean private ArticleBookmarkRepository articleBookmarkRepository;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  private Article article;
  private User anotherUser;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    anotherUser = new User("other@test.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), anotherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
  }

  private void stubArticleData(boolean bookmarked) {
    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            article.getTitle(),
            article.getDescription(),
            article.getBody(),
            false,
            0,
            bookmarked,
            article.getCreatedAt(),
            article.getUpdatedAt(),
            article.getTags().stream().map(Tag::getName).collect(Collectors.toList()),
            new ProfileData(
                anotherUser.getId(),
                anotherUser.getUsername(),
                anotherUser.getBio(),
                anotherUser.getImage(),
                false));
    when(articleQueryService.findBySlug(eq(article.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));
  }

  // AC-1
  @Test
  public void ac1_should_bookmark_an_article_success() throws Exception {
    stubArticleData(true);
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/bookmark", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.id", equalTo(article.getId()))
        .body("article.bookmarked", equalTo(true))
        .body("article.favorited", equalTo(false));

    verify(articleBookmarkRepository).save(new ArticleBookmark(article.getId(), user.getId()));
  }

  // AC-2
  @Test
  public void ac2_should_bookmark_idempotently() throws Exception {
    stubArticleData(true);
    for (int i = 0; i < 2; i++) {
      given()
          .header("Authorization", "Token " + token)
          .when()
          .post("/articles/{slug}/bookmark", article.getSlug())
          .then()
          .statusCode(200)
          .body("article.bookmarked", equalTo(true));
    }
    verify(articleBookmarkRepository, times(2))
        .save(new ArticleBookmark(article.getId(), user.getId()));
  }

  // AC-3
  @Test
  public void ac3_should_unbookmark_an_article_success() throws Exception {
    stubArticleData(false);
    ArticleBookmark bookmark = new ArticleBookmark(article.getId(), user.getId());
    when(articleBookmarkRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(bookmark));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.id", equalTo(article.getId()))
        .body("article.bookmarked", equalTo(false));
    verify(articleBookmarkRepository).remove(bookmark);
  }

  // AC-4
  @Test
  public void ac4_should_unbookmark_idempotently_when_not_bookmarked() throws Exception {
    stubArticleData(false);
    when(articleBookmarkRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", article.getSlug())
        .then()
        .statusCode(200)
        .body("article.bookmarked", equalTo(false));
    verify(articleBookmarkRepository, never()).remove(any());
  }

  // AC-5
  @Test
  public void ac5_should_return_401_when_bookmarking_without_token() throws Exception {
    given().when().post("/articles/{slug}/bookmark", article.getSlug()).then().statusCode(401);
    verify(articleBookmarkRepository, never()).save(any());
  }

  // AC-5
  @Test
  public void ac5_should_return_401_when_unbookmarking_without_token() throws Exception {
    given().when().delete("/articles/{slug}/bookmark", article.getSlug()).then().statusCode(401);
    verify(articleBookmarkRepository, never()).remove(any());
  }

  // AC-6
  @Test
  public void ac6_should_return_404_when_bookmarking_unknown_article() throws Exception {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/bookmark", "missing")
        .then()
        .statusCode(404);
    verify(articleBookmarkRepository, never()).save(any());
  }

  // AC-6
  @Test
  public void ac6_should_return_404_when_unbookmarking_unknown_article() throws Exception {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/bookmark", "missing")
        .then()
        .statusCode(404);
    verify(articleBookmarkRepository, never()).remove(any());
  }
}
