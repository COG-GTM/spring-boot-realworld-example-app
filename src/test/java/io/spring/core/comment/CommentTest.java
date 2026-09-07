package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_construct_comment() {
    Comment comment = new Comment("body", "user-id", "article-id");

    assertThat(comment.getId() != null, is(true));
    assertThat(comment.getCreatedAt() != null, is(true));
    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("user-id"));
    assertThat(comment.getArticleId(), is("article-id"));
  }

  @Test
  public void should_be_equal_by_id() {
    Comment first = new Comment("body", "user-id", "article-id");
    Comment second = new Comment("body", "user-id", "article-id");

    assertThat(first.equals(second), is(false));
    assertThat(first.equals(first), is(true));
  }
}
