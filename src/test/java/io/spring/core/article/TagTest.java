package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    assertNotNull(tag.getId());
    assertEquals("java", tag.getName());
  }

  @Test
  public void should_generate_unique_ids() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("spring");
    assertNotEquals(t1.getId(), t2.getId());
  }

  @Test
  public void should_have_equality_based_on_name() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("java");
    assertEquals(t1, t2);
  }

  @Test
  public void should_not_be_equal_with_different_names() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("spring");
    assertNotEquals(t1, t2);
  }
}
