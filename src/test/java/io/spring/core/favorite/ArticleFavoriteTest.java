package io.spring.core.favorite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteTest {

  @Test
  public void should_set_article_id_and_user_id_on_construction() {
    ArticleFavorite favorite = new ArticleFavorite("articleId", "userId");
    assertThat(favorite.getArticleId(), is("articleId"));
    assertThat(favorite.getUserId(), is("userId"));
  }

  @Test
  public void should_be_equal_for_same_article_and_user() {
    ArticleFavorite first = new ArticleFavorite("articleId", "userId");
    ArticleFavorite second = new ArticleFavorite("articleId", "userId");
    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_fields_differ() {
    ArticleFavorite first = new ArticleFavorite("articleId", "userId");
    ArticleFavorite second = new ArticleFavorite("articleId", "otherUser");
    assertThat(first, is(not(second)));
  }
}
