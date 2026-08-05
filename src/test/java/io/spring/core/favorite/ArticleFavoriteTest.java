package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_set_all_fields_from_constructor() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertThat(favorite.getArticleId(), is("article-1"));
    assertThat(favorite.getUserId(), is("user-1"));
  }

  @Test
  public void should_leave_all_fields_null_with_default_constructor() {
    ArticleFavorite favorite = new ArticleFavorite();

    assertThat(favorite.getArticleId(), is((String) null));
    assertThat(favorite.getUserId(), is((String) null));
  }

  @Test
  public void should_accept_null_and_empty_values() {
    ArticleFavorite favorite = new ArticleFavorite("", null);

    assertThat(favorite.getArticleId(), is(""));
    assertThat(favorite.getUserId(), is((String) null));
  }

  @Test
  public void should_be_equal_when_both_fields_are_equal() {
    ArticleFavorite first = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite second = new ArticleFavorite("article-1", "user-1");

    assertThat(first, is(second));
    assertThat(second, is(first));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_article_id_differs() {
    ArticleFavorite first = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite second = new ArticleFavorite("article-2", "user-1");

    assertNotEquals(first, second);
  }

  @Test
  public void should_not_be_equal_when_user_id_differs() {
    ArticleFavorite first = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite second = new ArticleFavorite("article-1", "user-2");

    assertNotEquals(first, second);
  }

  @Test
  public void should_not_treat_swapped_ids_as_equal() {
    ArticleFavorite first = new ArticleFavorite("article-1", "user-1");
    ArticleFavorite second = new ArticleFavorite("user-1", "article-1");

    assertNotEquals(first, second);
  }

  @Test
  public void should_be_equal_when_both_are_empty() {
    ArticleFavorite first = new ArticleFavorite();
    ArticleFavorite second = new ArticleFavorite();

    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertThat(favorite.equals(null), is(false));
    assertThat(favorite.equals("article-1"), is(false));
  }

  @Test
  public void should_be_equal_to_itself() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertThat(favorite.equals(favorite), is(true));
    assertThat(favorite.hashCode(), is(favorite.hashCode()));
  }
}
