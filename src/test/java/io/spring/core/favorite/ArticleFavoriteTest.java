package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite.getArticleId(), is("article-id"));
    assertThat(favorite.getUserId(), is("user-id"));
  }

  @Test
  public void should_compare_favorites_by_article_and_user() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");

    assertThat(favorite, is(new ArticleFavorite("article-id", "user-id")));
    assertThat(favorite.hashCode(), is(new ArticleFavorite("article-id", "user-id").hashCode()));
    assertThat(favorite, not(new ArticleFavorite("article-id", "other-user")));
    assertThat(favorite, not(new ArticleFavorite("other-article", "user-id")));
  }
}
