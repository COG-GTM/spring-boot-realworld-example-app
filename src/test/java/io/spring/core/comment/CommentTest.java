package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_all_fields_and_generate_metadata_on_construction() {
    Comment comment = new Comment("a comment body", "userId", "articleId");
    assertThat(comment.getId(), is(notNullValue()));
    assertThat(comment.getBody(), is("a comment body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
    assertThat(comment.getCreatedAt(), is(notNullValue()));
  }

  @Test
  public void should_generate_unique_ids_for_different_comments() {
    Comment first = new Comment("body", "userId", "articleId");
    Comment second = new Comment("body", "userId", "articleId");
    assertThat(first.getId(), is(not(second.getId())));
  }

  @Test
  public void should_be_equal_when_ids_are_equal() {
    Comment comment = new Comment("body", "userId", "articleId");
    assertThat(comment, is(comment));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_ids_differ() {
    Comment first = new Comment("body", "userId", "articleId");
    Comment second = new Comment("body", "userId", "articleId");
    assertThat(first, is(not(second)));
  }
}
