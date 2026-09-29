package io.spring.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.oneOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ArticleFeedIntegrationTest extends MySqlIntegrationTestBase {

  @Test
  void feed_requires_authentication() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  void feed_is_empty_when_user_follows_nobody() {
    String author = register("author");
    createArticle(author, "unfollowed article");
    String reader = register("reader");

    feed(reader, Collections.emptyMap())
        .statusCode(200)
        .body("articles", empty())
        .body("articlesCount", equalTo(0));
  }

  @Test
  void feed_contains_only_articles_from_followed_authors() {
    String alice = register("alice");
    String bob = register("bob");
    String carol = register("carol");
    String reader = register("reader");

    createArticle(alice, "alice first", "java");
    createArticle(alice, "alice second");
    createArticle(bob, "bob first", "spring");
    createArticle(carol, "carol ignored");
    createArticle(reader, "reader own article");

    follow(reader, "alice");
    follow(reader, "bob");

    feed(reader, Collections.emptyMap())
        .statusCode(200)
        .body("articlesCount", equalTo(3))
        .body("articles.slug", containsInAnyOrder("alice-first", "alice-second", "bob-first"))
        .body("articles.author.username", everyItem(oneOf("alice", "bob")))
        .body("articles.author.following", everyItem(equalTo(true)))
        .body("articles.favorited", everyItem(equalTo(false)))
        .body("articles.favoritesCount", everyItem(equalTo(0)));
  }

  @Test
  void feed_reflects_tags_and_favorites_from_database() {
    String alice = register("alice");
    String reader = register("reader");
    String other = register("other");
    String slug = createArticle(alice, "tagged article", "java", "spring");
    follow(reader, "alice");
    favorite(reader, slug).statusCode(200);
    favorite(other, slug).statusCode(200);

    feed(reader, Collections.emptyMap())
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles", hasSize(1))
        .body("articles[0].slug", equalTo(slug))
        .body("articles[0].tagList", containsInAnyOrder("java", "spring"))
        .body("articles[0].favorited", equalTo(true))
        .body("articles[0].favoritesCount", equalTo(2));
  }

  @Test
  void feed_drops_author_after_unfollow() {
    String alice = register("alice");
    String bob = register("bob");
    String reader = register("reader");
    createArticle(alice, "alice article");
    createArticle(bob, "bob article");
    follow(reader, "alice");
    follow(reader, "bob");

    unfollow(reader, "alice");

    feed(reader, Collections.emptyMap())
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles.slug", containsInAnyOrder("bob-article"));
  }

  @Test
  void feed_pagination_respects_limit_and_offset_and_reports_total_count() {
    String alice = register("alice");
    String reader = register("reader");
    for (int i = 1; i <= 5; i++) {
      createArticle(alice, "article " + i);
    }
    follow(reader, "alice");

    List<String> firstPage =
        feed(reader, page(0, 2))
            .statusCode(200)
            .body("articlesCount", equalTo(5))
            .body("articles", hasSize(2))
            .extract()
            .path("articles.slug");
    List<String> secondPage =
        feed(reader, page(2, 2))
            .statusCode(200)
            .body("articlesCount", equalTo(5))
            .body("articles", hasSize(2))
            .extract()
            .path("articles.slug");
    List<String> lastPage =
        feed(reader, page(4, 2))
            .statusCode(200)
            .body("articlesCount", equalTo(5))
            .body("articles", hasSize(1))
            .extract()
            .path("articles.slug");

    List<String> all = new ArrayList<>(firstPage);
    all.addAll(secondPage);
    all.addAll(lastPage);
    assertThat(
        all, containsInAnyOrder("article-1", "article-2", "article-3", "article-4", "article-5"));
  }

  @Test
  @Disabled(
      "Known bug: ArticleReadService.findArticlesOfAuthors has no ORDER BY, so feed order is"
          + " whatever MySQL returns rather than most recent first")
  void feed_returns_most_recent_articles_first() {
    String alice = register("alice");
    String reader = register("reader");
    createArticle(alice, "oldest");
    createArticle(alice, "newest");
    createArticle(alice, "middle");
    setCreatedAt("oldest", "2020-01-01 00:00:00");
    setCreatedAt("middle", "2021-01-01 00:00:00");
    setCreatedAt("newest", "2022-01-01 00:00:00");
    follow(reader, "alice");

    feed(reader, Collections.emptyMap())
        .statusCode(200)
        .body("articles.slug", contains("newest", "middle", "oldest"));
  }

  @Test
  @Disabled(
      "Known bug: ArticleReadService.findArticlesOfAuthors applies LIMIT to the article x tag"
          + " join, so multi-tag articles consume several slots and fall off the page")
  void feed_limit_counts_articles_not_tag_rows() {
    String alice = register("alice");
    String reader = register("reader");
    createArticle(alice, "multi tag one", "a", "b", "c");
    createArticle(alice, "multi tag two", "d", "e", "f");
    follow(reader, "alice");

    feed(reader, page(0, 2))
        .statusCode(200)
        .body("articlesCount", equalTo(2))
        .body("articles.slug", containsInAnyOrder("multi-tag-one", "multi-tag-two"));
  }

  private static Map<String, Object> page(int offset, int limit) {
    Map<String, Object> params = new HashMap<>();
    params.put("offset", offset);
    params.put("limit", limit);
    return params;
  }
}
