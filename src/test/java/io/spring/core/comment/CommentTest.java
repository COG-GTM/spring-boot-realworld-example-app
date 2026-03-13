package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_fields_via_constructor() {
    DateTime before = new DateTime();
    Comment comment = new Comment("body", "userId", "articleId");
    DateTime after = new DateTime();

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
    assertThat(comment.getCreatedAt(), notNullValue());
    assertThat(comment.getCreatedAt().isAfter(before) || comment.getCreatedAt().isEqual(before),
        is(true));
    assertThat(comment.getCreatedAt().isBefore(after) || comment.getCreatedAt().isEqual(after),
        is(true));
  }

  @Test
  public void should_equal_when_same_id() {
    Comment comment1 = new Comment("body1", "user1", "article1");
    Comment comment2 = new Comment("body2", "user2", "article2");
    assertThat(comment1.equals(comment1), is(true));
    assertThat(comment1.equals(comment2), is(false));
  }

  @Test
  public void should_have_same_hashcode_for_same_object() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }
}
