package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_keep_constructor_values() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    assertThat(favorite.getArticleId(), is("articleId"));
    assertThat(favorite.getUserId(), is("userId"));
  }

  @Test
  public void should_be_equal_when_all_fields_are_equal() {
    ArticleFavorite one = new ArticleFavorite("articleId", "userId");
    ArticleFavorite two = new ArticleFavorite("articleId", "userId");
    ArticleFavorite different = new ArticleFavorite("articleId", "other");
    assertThat(one.equals(two), is(true));
    assertThat(one.hashCode(), is(two.hashCode()));
    assertThat(one.equals(different), is(false));
  }
}
