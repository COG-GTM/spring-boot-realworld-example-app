package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_create_tag_with_name() {
    Tag tag = new Tag("java");
    assertEquals("java", tag.getName());
    assertNotNull(tag.getId());
  }

  @Test
  public void should_have_equals_based_on_name() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("java");
    assertEquals(t1, t2);
  }

  @Test
  public void should_not_be_equal_for_different_names() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("spring");
    assertNotEquals(t1, t2);
  }

  @Test
  public void should_have_consistent_hashcode_based_on_name() {
    Tag t1 = new Tag("java");
    Tag t2 = new Tag("java");
    assertEquals(t1.hashCode(), t2.hashCode());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    Tag tag = new Tag();
    assertNull(tag.getName());
    assertNull(tag.getId());
  }

  @Test
  public void should_set_name() {
    Tag tag = new Tag();
    tag.setName("spring");
    assertEquals("spring", tag.getName());
  }

  @Test
  public void should_set_id() {
    Tag tag = new Tag();
    tag.setId("custom-id");
    assertEquals("custom-id", tag.getId());
  }

  @Test
  public void should_have_to_string() {
    Tag tag = new Tag("java");
    assertNotNull(tag.toString());
    assertTrue(tag.toString().contains("java"));
  }
}
