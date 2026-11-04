package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_create_comment_with_generated_id_and_creation_time() {
    DateTime before = new DateTime();

    Comment comment = new Comment("content", "user-id", "article-id");

    assertThat(comment.getId(), notNullValue());
    assertThat(comment.getBody(), is("content"));
    assertThat(comment.getUserId(), is("user-id"));
    assertThat(comment.getArticleId(), is("article-id"));
    assertThat(comment.getCreatedAt(), notNullValue());
    assertThat(comment.getCreatedAt().isBefore(before), is(false));
  }

  @Test
  public void should_compare_comments_by_id() {
    Comment comment1 = new Comment("content", "user-id", "article-id");
    Comment comment2 = new Comment("content", "user-id", "article-id");

    assertThat(comment1, is(comment1));
    assertThat(comment1, not(comment2));
    assertThat(comment1.getId(), not(comment2.getId()));
  }
}
