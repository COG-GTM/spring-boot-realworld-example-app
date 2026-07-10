package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_set_all_fields_in_constructor() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertEquals("article-1", favorite.getArticleId());
    assertEquals("user-1", favorite.getUserId());
  }

  @Test
  public void should_be_equal_and_share_hashcode_when_all_fields_are_equal() {
    ArticleFavorite first = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite second = new ArticleFavorite("article-1", "user-1");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_any_field_differs() {
    ArticleFavorite base = new ArticleFavorite("article-1", "user-1");

    assertNotEquals(base, new ArticleFavorite("article-2", "user-1"));
    assertNotEquals(base, new ArticleFavorite("article-1", "user-2"));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertNotEquals(favorite, null);
    assertNotEquals(favorite, "a string");
    assertTrue(favorite.equals(favorite));
  }
}
