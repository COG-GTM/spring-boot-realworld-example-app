package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_assign_article_and_user_ids() {
    ArticleFavorite favorite = new ArticleFavorite("article-1", "user-1");

    assertThat(favorite.getArticleId(), is("article-1"));
    assertThat(favorite.getUserId(), is("user-1"));
  }

  @Test
  public void should_compare_by_all_fields() {
    ArticleFavorite a = new ArticleFavorite("article-1", "user-1");

    assertThat(a, is(new ArticleFavorite("article-1", "user-1")));
    assertThat(a.hashCode(), is(new ArticleFavorite("article-1", "user-1").hashCode()));
    assertThat(a, is(not(new ArticleFavorite("article-1", "user-2"))));
    assertThat(a, is(not(new ArticleFavorite("article-2", "user-1"))));
  }
}
