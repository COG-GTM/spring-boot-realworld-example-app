package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_assign_all_fields_from_constructor() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite.getArticleId(), is("articleId"));
    assertThat(favorite.getUserId(), is("userId"));
  }

  @Test
  public void should_have_null_fields_when_created_with_default_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId(), is(nullValue()));
    assertThat(favorite.getUserId(), is(nullValue()));
  }

  @Test
  public void should_be_equal_when_both_ids_are_equal() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    ArticleFavorite same = new ArticleFavorite("articleId", "userId");

    assertThat(favorite, is(same));
    assertThat(favorite.hashCode(), is(same.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_any_id_differs() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite, is(not(new ArticleFavorite("otherArticleId", "userId"))));
    assertThat(favorite, is(not(new ArticleFavorite("articleId", "otherUserId"))));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");

    assertThat(favorite.equals(null), is(false));
    assertThat(favorite.equals("articleId"), is(false));
  }
}
