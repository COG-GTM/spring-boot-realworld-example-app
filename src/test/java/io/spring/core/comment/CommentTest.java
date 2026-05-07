package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment() {
    Comment comment = new Comment("body", "user-id", "article-id");
    assertEquals("body", comment.getBody());
    assertEquals("user-id", comment.getUserId());
    assertEquals("article-id", comment.getArticleId());
    assertNotNull(comment.getId());
    assertNotNull(comment.getCreatedAt());
  }

  @Test
  public void should_have_equals_based_on_id() {
    Comment c1 = new Comment("body1", "user1", "article1");
    Comment c2 = new Comment("body2", "user2", "article2");
    assertNotEquals(c1, c2);
    assertEquals(c1, c1);
  }

  @Test
  public void should_have_consistent_hashcode() {
    Comment comment = new Comment("body", "user-id", "article-id");
    assertEquals(comment.hashCode(), comment.hashCode());
  }
}
