package io.spring.core.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CommentTest {

  private static void setId(Comment comment, String id) throws Exception {
    Field field = Comment.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(comment, id);
  }

  @Test
  public void should_set_all_fields_and_generate_uuid_id_in_constructor() {
    Comment comment = new Comment("nice article", "user-1", "article-1");

    assertEquals("nice article", comment.getBody());
    assertEquals("user-1", comment.getUserId());
    assertEquals("article-1", comment.getArticleId());
    assertNotNull(comment.getCreatedAt());
    assertNotNull(comment.getId());
    assertEquals(comment.getId(), UUID.fromString(comment.getId()).toString());
  }

  @Test
  public void should_generate_distinct_ids_for_distinct_comments() {
    Comment first = new Comment("body", "user-1", "article-1");
    Comment second = new Comment("body", "user-1", "article-1");

    assertNotEquals(first.getId(), second.getId());
  }

  @Test
  public void should_be_equal_and_share_hashcode_when_ids_are_equal() throws Exception {
    Comment first = new Comment("body-a", "user-a", "article-a");
    Comment second = new Comment("body-b", "user-b", "article-b");
    setId(first, "same-id");
    setId(second, "same-id");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_differ() throws Exception {
    Comment first = new Comment("body", "user-1", "article-1");
    Comment second = new Comment("body", "user-1", "article-1");
    setId(first, "id-1");
    setId(second, "id-2");

    assertNotEquals(first, second);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertNotEquals(comment, null);
    assertNotEquals(comment, "a string");
    assertTrue(comment.equals(comment));
  }
}
