package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_generate_id_and_created_at_and_keep_values() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.getId(), is(notNullValue()));
    assertThat(comment.getCreatedAt(), is(notNullValue()));
    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
  }

  @Test
  public void should_generate_distinct_ids() {
    Comment one = new Comment("body", "userId", "articleId");
    Comment two = new Comment("body", "userId", "articleId");
    assertThat(one.getId().equals(two.getId()), is(false));
  }

  @Test
  public void should_be_equal_when_ids_are_equal() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.equals(comment), is(true));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }
}
