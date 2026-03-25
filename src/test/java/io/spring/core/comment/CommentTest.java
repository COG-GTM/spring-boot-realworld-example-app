package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("body text", "user1", "article1");
    assertNotNull(comment.getId());
    assertEquals("body text", comment.getBody());
    assertEquals("user1", comment.getUserId());
    assertEquals("article1", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_generate_unique_ids() {
    Comment c1 = new Comment("body1", "user1", "article1");
    Comment c2 = new Comment("body2", "user2", "article2");
    assertNotEquals(c1.getId(), c2.getId());
  }

  @Test
  public void should_equal_by_id() {
    Comment c1 = new Comment("body1", "user1", "article1");
    Comment c2 = new Comment("body2", "user2", "article2");
    assertNotEquals(c1, c2);
    assertEquals(c1, c1);
  }

  @Test
  public void should_have_consistent_hashcode() {
    Comment comment = new Comment("body", "user1", "article1");
    assertEquals(comment.hashCode(), comment.hashCode());
  }

  @Test
  public void should_create_comment_with_no_arg_constructor() {
    Comment comment = new Comment();
    assertNull(comment.getId());
    assertNull(comment.getBody());
  }
}
