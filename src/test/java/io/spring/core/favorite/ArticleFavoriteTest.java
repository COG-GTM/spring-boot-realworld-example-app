package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_ids() {
    ArticleFavorite favorite = new ArticleFavorite("article-123", "user-456");

    assertEquals("article-123", favorite.getArticleId());
    assertEquals("user-456", favorite.getUserId());
  }

  @Test
  public void should_have_equals_based_on_all_fields() {
    ArticleFavorite favorite1 = new ArticleFavorite("article-123", "user-456");
    ArticleFavorite favorite2 = new ArticleFavorite("article-123", "user-456");
    ArticleFavorite favorite3 = new ArticleFavorite("article-123", "user-789");
    ArticleFavorite favorite4 = new ArticleFavorite("article-999", "user-456");

    assertEquals(favorite1, favorite2);
    assertNotEquals(favorite1, favorite3);
    assertNotEquals(favorite1, favorite4);
  }

  @Test
  public void should_have_same_hashcode_for_equal_objects() {
    ArticleFavorite favorite1 = new ArticleFavorite("article-123", "user-456");
    ArticleFavorite favorite2 = new ArticleFavorite("article-123", "user-456");

    assertEquals(favorite1.hashCode(), favorite2.hashCode());
  }

  @Test
  public void should_have_different_hashcode_for_different_objects() {
    ArticleFavorite favorite1 = new ArticleFavorite("article-123", "user-456");
    ArticleFavorite favorite2 = new ArticleFavorite("article-999", "user-789");

    assertNotEquals(favorite1.hashCode(), favorite2.hashCode());
  }
}
