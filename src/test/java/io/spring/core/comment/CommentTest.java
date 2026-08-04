package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_generate_id_timestamp_and_assign_fields() {
    Comment comment = new Comment("nice article", "user-1", "article-1");

    assertNotNull(comment.getId());
    assertNotNull(comment.getCreatedAt());
    assertThat(comment.getBody(), is("nice article"));
    assertThat(comment.getUserId(), is("user-1"));
    assertThat(comment.getArticleId(), is("article-1"));
  }

  @Test
  public void should_generate_distinct_ids() {
    Comment a = new Comment("b", "u", "a");
    Comment b = new Comment("b", "u", "a");

    assertThat(a.getId(), is(not(b.getId())));
  }

  @Test
  public void should_use_only_id_for_equality() {
    Comment a = new Comment("body", "user-1", "article-1");
    Comment b = new Comment("body", "user-1", "article-1");

    assertThat(a, is(a));
    assertThat(a, is(not(b)));
    assertThat(a.hashCode(), is(a.hashCode()));
  }
}
