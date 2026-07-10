package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_set_name_and_generate_uuid_id_in_constructor() {
    Tag tag = new Tag("java");

    assertEquals("java", tag.getName());
    assertNotNull(tag.getId());
    assertEquals(tag.getId(), UUID.fromString(tag.getId()).toString());
  }

  @Test
  public void should_expose_setters_from_data_annotation() {
    Tag tag = new Tag();
    tag.setId("id-1");
    tag.setName("spring");

    assertEquals("id-1", tag.getId());
    assertEquals("spring", tag.getName());
  }

  @Test
  public void should_be_equal_and_share_hashcode_when_names_are_equal() {
    Tag first = new Tag("java");
    Tag second = new Tag("java");

    assertNotEquals(first.getId(), second.getId());
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_names_differ() {
    Tag first = new Tag("java");
    Tag second = new Tag("spring");

    assertNotEquals(first, second);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    Tag tag = new Tag("java");

    assertNotEquals(tag, null);
    assertNotEquals(tag, "a string");
    assertTrue(tag.equals(tag));
  }
}
