package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("Comment body", "user-id", "article-id");

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("Comment body"));
    assertThat(comment.getUserId(), is("user-id"));
    assertThat(comment.getArticleId(), is("article-id"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_create_empty_comment() {
    Comment comment = new Comment();

    assertNotNull(comment);
  }

  @Test
  public void should_generate_unique_ids() {
    Comment comment1 = new Comment("Body 1", "user-id", "article-id");
    Comment comment2 = new Comment("Body 2", "user-id", "article-id");

    assertNotEquals(comment1.getId(), comment2.getId());
  }

  @Test
  public void should_implement_equals_based_on_id() {
    Comment comment1 = new Comment("Body", "user-id", "article-id");
    Comment comment2 = new Comment("Different Body", "different-user", "different-article");

    assertNotEquals(comment1, comment2);
  }

  @Test
  public void should_implement_hashcode_based_on_id() {
    Comment comment1 = new Comment("Body", "user-id", "article-id");
    Comment comment2 = new Comment("Body", "user-id", "article-id");

    assertNotEquals(comment1.hashCode(), comment2.hashCode());
  }
}
