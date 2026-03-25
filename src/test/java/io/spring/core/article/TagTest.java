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
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertNotEquals(tag1.getId(), tag2.getId());
  }

  @Test
  public void should_equal_by_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertEquals(tag1, tag2);
  }

  @Test
  public void should_not_equal_different_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("spring");
    assertNotEquals(tag1, tag2);
  }

  @Test
  public void should_have_same_hashcode_for_same_name() {
    Tag tag1 = new Tag("java");
    Tag tag2 = new Tag("java");
    assertEquals(tag1.hashCode(), tag2.hashCode());
  }

  @Test
  public void should_create_tag_with_no_arg_constructor() {
    Tag tag = new Tag();
    assertNull(tag.getId());
    assertNull(tag.getName());
  }

  @Test
  public void should_set_name() {
    Tag tag = new Tag();
    tag.setName("python");
    assertEquals("python", tag.getName());
  }

  @Test
  public void should_set_id() {
    Tag tag = new Tag();
    tag.setId("custom-id");
    assertEquals("custom-id", tag.getId());
  }
}
