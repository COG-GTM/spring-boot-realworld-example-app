package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_assign_all_fields_from_constructor() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment.getBody(), is("body"));
    assertThat(comment.getUserId(), is("userId"));
    assertThat(comment.getArticleId(), is("articleId"));
  }

  @Test
  public void should_generate_uuid_as_id() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment.getId(), is(notNullValue()));
    assertThat(UUID.fromString(comment.getId()).toString(), is(comment.getId()));
  }

  @Test
  public void should_generate_distinct_ids_for_different_comments() {
    Comment comment = new Comment("body", "userId", "articleId");
    Comment other = new Comment("body", "userId", "articleId");

    assertThat(comment.getId(), is(not(other.getId())));
  }

  @Test
  public void should_set_created_at_to_current_time() {
    DateTime before = new DateTime();
    Comment comment = new Comment("body", "userId", "articleId");
    DateTime after = new DateTime();

    assertThat(comment.getCreatedAt(), is(notNullValue()));
    assertThat(comment.getCreatedAt().isBefore(before), is(false));
    assertThat(comment.getCreatedAt().isAfter(after), is(false));
  }

  @Test
  public void should_have_null_fields_when_created_with_default_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId(), is(nullValue()));
    assertThat(comment.getBody(), is(nullValue()));
    assertThat(comment.getUserId(), is(nullValue()));
    assertThat(comment.getArticleId(), is(nullValue()));
    assertThat(comment.getCreatedAt(), is(nullValue()));
  }

  @Test
  public void should_not_be_equal_when_ids_differ() {
    Comment comment = new Comment("body", "userId", "articleId");
    Comment other = new Comment("body", "userId", "articleId");

    assertThat(comment, is(not(other)));
  }

  @Test
  public void should_be_equal_to_itself() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment, is(comment));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }

  @Test
  public void should_be_equal_when_ids_are_equal() {
    Comment comment = new Comment();
    Comment other = new Comment();

    assertThat(comment, is(other));
    assertThat(comment.hashCode(), is(other.hashCode()));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    Comment comment = new Comment("body", "userId", "articleId");

    assertThat(comment.equals(null), is(false));
    assertThat(comment.equals("body"), is(false));
  }
}
