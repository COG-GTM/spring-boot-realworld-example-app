package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    assertNotNull(tag.getId());
    assertEquals("java", tag.getName());
  }

  @Test
  void should_generate_unique_ids() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertNotEquals(tag1.getId(), tag2.getId());
  }

  @Test
  void should_have_equality_based_on_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertEquals(tag1, tag2);
  }

  @Test
  void should_not_equal_different_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertNotEquals(tag1, tag2);
  }

  @Test
  void should_support_setters() {
    Tag tag = new Tag("java");
    tag.setName("spring");
    assertEquals("spring", tag.getName());
  }
}
