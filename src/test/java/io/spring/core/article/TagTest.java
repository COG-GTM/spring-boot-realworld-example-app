package io.spring.core.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class TagTest {

  @Test
  public void should_generate_id_on_creation() {
    Tag tag = new Tag("java");
    assertNotNull(tag.getId());
    assertEquals("java", tag.getName());
  }

  @Test
  public void should_compare_tags_by_name() {
    Tag a = new Tag("java");
    Tag b = new Tag("java");
    assertNotEquals(a.getId(), b.getId());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, new Tag("spring"));
  }

  @Test
  public void article_should_deduplicate_tags() {
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java", "java", "spring"), "123");
    assertEquals(2, article.getTags().size());
  }
}
