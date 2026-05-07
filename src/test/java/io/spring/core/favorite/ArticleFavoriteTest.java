package io.spring.core.favorite;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite fav = new ArticleFavorite("article-id", "user-id");
    assertEquals("article-id", fav.getArticleId());
    assertEquals("user-id", fav.getUserId());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    ArticleFavorite f1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite f2 = new ArticleFavorite("article1", "user1");
    assertEquals(f1, f2);
    assertEquals(f1.hashCode(), f2.hashCode());
  }

  @Test
  public void should_not_be_equal_for_different_favorites() {
    ArticleFavorite f1 = new ArticleFavorite("article1", "user1");
    ArticleFavorite f2 = new ArticleFavorite("article2", "user1");
    assertNotEquals(f1, f2);
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    ArticleFavorite fav = new ArticleFavorite();
    assertNull(fav.getArticleId());
    assertNull(fav.getUserId());
  }
}
