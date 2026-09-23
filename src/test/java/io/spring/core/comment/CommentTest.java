package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_all_fields_on_creation() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment.getId(), is(notNullValue()));
    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
    assertThat(comment.getCreatedAt(), is(notNullValue()));
  }

  @Test
  public void should_be_equal_only_by_id() {
    Comment comment = new Comment("body", "userId", "articleId");
    Comment another = new Comment("body", "userId", "articleId");

    assertThat(comment.equals(another), is(false));
    assertThat(comment.equals(comment), is(true));
    assertThat(comment.getId(), is(not(another.getId())));
  }
}
