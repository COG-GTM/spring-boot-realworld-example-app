package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_correct_fields() {
    Comment comment = new Comment("comment body", "user-id-1", "article-id-1");
    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getId().length(), is(36));
    assertThat(comment.getBody(), is("comment body"));
    assertThat(comment.getUserId(), is("user-id-1"));
    assertThat(comment.getArticleId(), is("article-id-1"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_generate_unique_ids() {
    Comment c1 = new Comment("body1", "u1", "a1");
    Comment c2 = new Comment("body2", "u2", "a2");
    assertThat(c1.getId().equals(c2.getId()), is(false));
  }

  @Test
  public void should_be_equal_when_same_id() {
    Comment c1 = new Comment("body", "u1", "a1");
    assertThat(c1, is(c1));
  }
}
