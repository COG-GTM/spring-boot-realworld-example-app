package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_create_article_favorite_with_article_and_user() {
    ArticleFavorite articleFavorite = new ArticleFavorite("article123", "user456");

    assertThat(articleFavorite.getArticleId(), is("article123"));
    assertThat(articleFavorite.getUserId(), is("user456"));
  }

  @Test
  public void should_create_article_favorite_with_no_args_constructor() {
    ArticleFavorite articleFavorite = new ArticleFavorite();

    assertThat(articleFavorite.getArticleId(), nullValue());
    assertThat(articleFavorite.getUserId(), nullValue());
  }

  @Test
  public void should_have_equality_based_on_all_fields() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user456");

    assertThat(favorite1.equals(favorite2), is(true));
    assertThat(favorite1.hashCode(), is(favorite2.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_article_id_differs() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article789", "user456");

    assertThat(favorite1.equals(favorite2), is(false));
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article123", "user789");

    assertThat(favorite1.equals(favorite2), is(false));
  }

  @Test
  public void should_have_consistent_hashcode() {
    ArticleFavorite articleFavorite = new ArticleFavorite("article123", "user456");

    int hashCode1 = articleFavorite.hashCode();
    int hashCode2 = articleFavorite.hashCode();

    assertThat(hashCode1, is(hashCode2));
  }

  @Test
  public void should_have_different_hashcode_for_different_favorites() {
    ArticleFavorite favorite1 = new ArticleFavorite("article123", "user456");
    ArticleFavorite favorite2 = new ArticleFavorite("article789", "user012");

    assertThat(favorite1.hashCode(), not(favorite2.hashCode()));
  }

  @Test
  public void should_be_equal_to_itself() {
    ArticleFavorite articleFavorite = new ArticleFavorite("article123", "user456");

    assertThat(articleFavorite.equals(articleFavorite), is(true));
  }

  @Test
  public void should_not_be_equal_to_null() {
    ArticleFavorite articleFavorite = new ArticleFavorite("article123", "user456");

    assertThat(articleFavorite.equals(null), is(false));
  }

  @Test
  public void should_not_be_equal_to_different_type() {
    ArticleFavorite articleFavorite = new ArticleFavorite("article123", "user456");
    String differentType = "not an ArticleFavorite";

    assertThat(articleFavorite.equals(differentType), is(false));
  }

  @Test
  public void should_handle_empty_ids() {
    ArticleFavorite articleFavorite = new ArticleFavorite("", "");

    assertThat(articleFavorite.getArticleId(), is(""));
    assertThat(articleFavorite.getUserId(), is(""));
  }
}
