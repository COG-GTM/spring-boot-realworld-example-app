package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  void should_generate_uuid_and_set_name() {
    Tag tag = new Tag("java");
    assertNotNull(tag.getId());
    assertFalse(tag.getId().isEmpty());
    assertEquals("java", tag.getName());
  }

  @Test
  void should_be_equal_when_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertEquals(tag1, tag2);
    assertEquals(tag1.hashCode(), tag2.hashCode());
  }

  @Test
  void should_not_be_equal_when_different_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("python");
    assertNotEquals(tag1, tag2);
  }
}
