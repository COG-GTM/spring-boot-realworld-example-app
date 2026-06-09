package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_correct_fields() {
    ArticleFavorite fav = new ArticleFavorite("article-id", "user-id");
    assertThat(fav.getArticleId(), is("article-id"));
    assertThat(fav.getUserId(), is("user-id"));
  }

  @Test
  public void should_be_equal_when_same_article_and_user() {
    ArticleFavorite fav1 = new ArticleFavorite("article-id", "user-id");
    ArticleFavorite fav2 = new ArticleFavorite("article-id", "user-id");
    assertThat(fav1, is(fav2));
    assertThat(fav1.hashCode(), is(fav2.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_different_article_or_user() {
    ArticleFavorite fav1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite fav2 = new ArticleFavorite("article-2", "user-1");
    ArticleFavorite fav3 = new ArticleFavorite("article-1", "user-2");
    assertThat(fav1, not(fav2));
    assertThat(fav1, not(fav3));
  }
}
