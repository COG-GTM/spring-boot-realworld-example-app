package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("body text", "user123", "article456");
    assertNotNull(comment.getId());
    assertEquals("body text", comment.getBody());
    assertEquals("user123", comment.getUserId());
    assertEquals("article456", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_generate_unique_ids() {
    Comment c1 = new Comment("body1", "user1", "article1");
    Comment c2 = new Comment("body2", "user2", "article2");
    assertNotEquals(c1.getId(), c2.getId());
  }

  @Test
  public void should_have_equality_based_on_id() {
    Comment c1 = new Comment("body", "user", "article");
    Comment c2 = new Comment("body", "user", "article");
    assertNotEquals(c1, c2);
    assertEquals(c1, c1);
  }
}
