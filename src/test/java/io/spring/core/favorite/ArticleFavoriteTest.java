package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");
    assertThat(favorite.getArticleId(), is("article-1"));
    assertThat(favorite.getUserId(), is("user-1"));
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    ArticleFavorite f1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite f2 = new ArticleFavorite("article-1", "user-1");
    assertThat(f1.equals(f2), is(true));
  }

  @Test
  public void should_not_be_equal_with_different_article_id() {
    ArticleFavorite f1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite f2 = new ArticleFavorite("article-2", "user-1");
    assertThat(f1.equals(f2), is(false));
  }

  @Test
  public void should_not_be_equal_with_different_user_id() {
    ArticleFavorite f1 = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite f2 = new ArticleFavorite("article-1", "user-2");
    assertThat(f1.equals(f2), is(false));
  }
}
