package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteCountTest {

  @Test
  public void should_create_article_favorite_count() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("article-id", 10);

    assertThat(count.getId(), is("article-id"));
    assertThat(count.getCount(), is(10));
  }

  @Test
  public void should_create_article_favorite_count_with_zero() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("article-id", 0);

    assertThat(count.getId(), is("article-id"));
    assertThat(count.getCount(), is(0));
  }

  @Test
  public void should_implement_equals_and_hashcode() {
    ArticleFavoriteCount count1 = new ArticleFavoriteCount("article-id", 10);
    ArticleFavoriteCount count2 = new ArticleFavoriteCount("article-id", 10);
    ArticleFavoriteCount count3 = new ArticleFavoriteCount("other-id", 5);

    assertEquals(count1, count2);
    assertNotEquals(count1, count3);
    assertEquals(count1.hashCode(), count2.hashCode());
  }

  @Test
  public void should_implement_toString() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("article-id", 10);

    String toString = count.toString();
    assertThat(toString.contains("article-id"), is(true));
    assertThat(toString.contains("10"), is(true));
  }
}
