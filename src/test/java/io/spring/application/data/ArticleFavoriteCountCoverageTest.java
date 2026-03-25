package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ArticleFavoriteCountCoverageTest {

  @Test
  public void should_create_with_id_and_count() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("id1", 5);

    assertEquals("id1", count.getId());
    assertEquals(5, count.getCount());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    ArticleFavoriteCount count1 = new ArticleFavoriteCount("id1", 5);
    ArticleFavoriteCount count2 = new ArticleFavoriteCount("id1", 5);

    assertEquals(count1, count2);
    assertEquals(count1.hashCode(), count2.hashCode());
  }

  @Test
  public void should_not_equal_different() {
    ArticleFavoriteCount count1 = new ArticleFavoriteCount("id1", 5);
    ArticleFavoriteCount count2 = new ArticleFavoriteCount("id2", 3);

    assertNotEquals(count1, count2);
  }

  @Test
  public void should_have_toString() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("id1", 5);
    assertNotNull(count.toString());
  }

  @Test
  public void should_create_with_zero_count() {
    ArticleFavoriteCount count = new ArticleFavoriteCount("id1", 0);
    assertEquals(0, count.getCount());
  }
}
