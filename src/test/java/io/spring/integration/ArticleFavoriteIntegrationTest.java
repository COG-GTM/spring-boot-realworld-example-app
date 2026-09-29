package io.spring.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteIntegrationTest extends MySqlIntegrationTestBase {

  @Test
  void favorite_requires_authentication() {
    String author = register("author");
    String slug = createArticle(author, "some article");

    given().when().post("/articles/{slug}/favorite", slug).then().statusCode(401);
    given().when().delete("/articles/{slug}/favorite", slug).then().statusCode(401);
  }

  @Test
  void favorite_unknown_article_returns_404() {
    String reader = register("reader");

    favorite(reader, "does-not-exist").statusCode(404);
    unfavorite(reader, "does-not-exist").statusCode(404);
  }

  @Test
  void favorite_persists_and_is_reflected_in_article_response() {
    String author = register("author");
    String reader = register("reader");
    String slug = createArticle(author, "favorite me");

    favorite(reader, slug)
        .statusCode(200)
        .body("article.slug", equalTo(slug))
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    assertFavoriteRows(slug, 1);
    given()
        .header("Authorization", "Token " + reader)
        .when()
        .get("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));
  }

  @Test
  void favoriting_twice_is_idempotent() {
    String author = register("author");
    String reader = register("reader");
    String slug = createArticle(author, "favorite twice");

    favorite(reader, slug).statusCode(200);
    favorite(reader, slug)
        .statusCode(200)
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    assertFavoriteRows(slug, 1);
  }

  @Test
  void favorites_count_aggregates_across_users_and_favorited_is_per_user() {
    String author = register("author");
    String first = register("first");
    String second = register("second");
    String bystander = register("bystander");
    String slug = createArticle(author, "popular article");

    favorite(first, slug).statusCode(200);
    favorite(second, slug).statusCode(200).body("article.favoritesCount", equalTo(2));

    given()
        .header("Authorization", "Token " + bystander)
        .when()
        .get("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(2));
  }

  @Test
  void unfavorite_removes_row_and_updates_response() {
    String author = register("author");
    String reader = register("reader");
    String other = register("other");
    String slug = createArticle(author, "unfavorite me");
    favorite(reader, slug).statusCode(200);
    favorite(other, slug).statusCode(200);

    unfavorite(reader, slug)
        .statusCode(200)
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(1));

    assertFavoriteRows(slug, 1);
  }

  @Test
  void unfavorite_without_prior_favorite_is_a_no_op() {
    String author = register("author");
    String reader = register("reader");
    String slug = createArticle(author, "never favorited");

    unfavorite(reader, slug)
        .statusCode(200)
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0));
  }

  @Test
  void list_articles_filtered_by_favorited_user() {
    String author = register("author");
    String reader = register("reader");
    String other = register("other");
    String liked = createArticle(author, "liked article", "java");
    String alsoLiked = createArticle(author, "also liked article");
    String likedByOther = createArticle(author, "liked by other");
    createArticle(author, "not liked");
    favorite(reader, liked).statusCode(200);
    favorite(reader, alsoLiked).statusCode(200);
    favorite(other, likedByOther).statusCode(200);
    favorite(other, liked).statusCode(200);

    given()
        .header("Authorization", "Token " + reader)
        .queryParam("favorited", "reader")
        .when()
        .get("/articles")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(2))
        .body("articles.slug", containsInAnyOrder(liked, alsoLiked))
        .body("articles.favorited", containsInAnyOrder(true, true))
        .body("articles.favoritesCount", containsInAnyOrder(2, 1));

    given()
        .queryParam("favorited", "nobody")
        .when()
        .get("/articles")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(0))
        .body("articles", empty());
  }

  private void assertFavoriteRows(String slug, int expected) {
    Integer rows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM article_favorites AF JOIN articles A ON A.id = AF.article_id"
                + " WHERE A.slug = ?",
            Integer.class,
            slug);
    assertThat(rows, equalTo(expected));
  }
}
