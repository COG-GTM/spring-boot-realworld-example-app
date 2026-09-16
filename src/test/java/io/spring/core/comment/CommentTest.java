package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_initialize_fields_on_creation() {
    Comment comment = new Comment("body", "user-id", "article-id");
    assertNotNull(comment.getId());
    assertNotNull(comment.getCreatedAt());
    assertEquals("body", comment.getBody());
    assertEquals("user-id", comment.getUserId());
    assertEquals("article-id", comment.getArticleId());
  }

  @Test
  public void should_generate_unique_ids() {
    assertNotEquals(new Comment("a", "u", "a").getId(), new Comment("a", "u", "a").getId());
  }

  @Test
  public void should_compare_by_id_only() {
    Comment comment = new Comment("body", "user-id", "article-id");
    Comment other = new Comment("body", "user-id", "article-id");
    assertNotEquals(comment, other);
    assertEquals(comment, comment);
  }
}
