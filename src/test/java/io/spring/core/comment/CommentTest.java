package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_fields_and_generate_id_and_timestamp() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertNotNull(comment.getId());
    UUID parsed = UUID.fromString(comment.getId());
    assertEquals(comment.getId(), parsed.toString());
    assertEquals("body", comment.getBody());
    assertEquals("user-1", comment.getUserId());
    assertEquals("article-1", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_generate_unique_ids() {
    Comment c1 = new Comment("body", "user-1", "article-1");
    Comment c2 = new Comment("body", "user-1", "article-1");
    assertNotEquals(c1.getId(), c2.getId());
  }
}
