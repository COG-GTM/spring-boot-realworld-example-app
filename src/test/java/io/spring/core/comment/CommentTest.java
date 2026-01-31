package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_all_fields() {
    Comment comment = new Comment("test body", "user123", "article456");
    
    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("test body"));
    assertThat(comment.getUserId(), is("user123"));
    assertThat(comment.getArticleId(), is("article456"));
    assertThat(comment.getCreatedAt(), notNullValue());
  }

  @Test
  public void should_have_equals_based_on_id() {
    Comment comment1 = new Comment("test body", "user123", "article456");
    Comment comment2 = new Comment("test body", "user123", "article456");
    
    assertThat(comment1.equals(comment2), is(false));
    assertThat(comment1.equals(comment1), is(true));
  }
}
