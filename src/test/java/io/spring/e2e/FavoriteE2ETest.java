package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class FavoriteE2ETest extends BaseE2ETest {

  @Test
  void should_favorite_article() {
    String authorToken = registerAndGetToken("favauthor@test.com", "favauthoruser", "password123");
    String userToken = registerAndGetToken("favuser@test.com", "favuseruser", "password123");

    Map<String, Object> param = articleParam("Favorite Article", "Desc", "Body", null);
    post("/articles", param, authorToken);

    ResponseEntity<String> response = post("/articles/favorite-article/favorite", null, userToken);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode article = body.get("article");
    assertTrue(article.get("favorited").asBoolean());
    assertEquals(1, article.get("favoritesCount").asInt());
  }

  @Test
  void should_unfavorite_article() {
    String authorToken =
        registerAndGetToken("unfavauthor@test.com", "unfavauthoruser", "password123");
    String userToken = registerAndGetToken("unfavuser@test.com", "unfavuseruser", "password123");

    Map<String, Object> param = articleParam("Unfavorite Article", "Desc", "Body", null);
    post("/articles", param, authorToken);

    // First favorite
    post("/articles/unfavorite-article/favorite", null, userToken);

    // Then unfavorite
    ResponseEntity<String> response = delete("/articles/unfavorite-article/favorite", userToken);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode article = body.get("article");
    assertFalse(article.get("favorited").asBoolean());
    assertEquals(0, article.get("favoritesCount").asInt());
  }

  @Test
  void should_increment_favorites_count() {
    String authorToken =
        registerAndGetToken("incrauthor@test.com", "incrauthoruser", "password123");
    String user1Token = registerAndGetToken("incruser1@test.com", "incruser1", "password123");
    String user2Token = registerAndGetToken("incruser2@test.com", "incruser2", "password123");

    Map<String, Object> param = articleParam("Multi Fav Article", "Desc", "Body", null);
    post("/articles", param, authorToken);

    post("/articles/multi-fav-article/favorite", null, user1Token);
    ResponseEntity<String> response =
        post("/articles/multi-fav-article/favorite", null, user2Token);

    JsonNode body = parseJson(response.getBody());
    assertEquals(2, body.get("article").get("favoritesCount").asInt());
  }

  @Test
  void should_fail_favorite_without_auth() {
    String authorToken =
        registerAndGetToken("noauthfavauthor@test.com", "noauthfavauthor", "password123");

    Map<String, Object> param = articleParam("No Auth Fav Article", "Desc", "Body", null);
    post("/articles", param, authorToken);

    HttpClientErrorException ex =
        expectClientError(HttpMethod.POST, "/articles/no-auth-fav-article/favorite", null, null);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getRawStatusCode());
  }

  @Test
  void should_list_articles_favorited_by_user() {
    String authorToken =
        registerAndGetToken("favlistauthor@test.com", "favlistauthor", "password123");
    String userToken = registerAndGetToken("favlistuser@test.com", "favlistuser", "password123");

    Map<String, Object> param = articleParam("Favorited List Article", "Desc", "Body", null);
    post("/articles", param, authorToken);

    post("/articles/favorited-list-article/favorite", null, userToken);

    ResponseEntity<String> response = get("/articles?favorited=favlistuser");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.get("articlesCount").asInt() >= 1);
  }
}
