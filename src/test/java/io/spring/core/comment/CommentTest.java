package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.Field;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_all_fields_from_constructor() {
    Comment comment = new Comment("a comment body", "user-1", "article-1");

    assertThat(comment.getBody(), is("a comment body"));
    assertThat(comment.getUserId(), is("user-1"));
    assertThat(comment.getArticleId(), is("article-1"));
  }

  @Test
  public void should_generate_a_uuid_as_id() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertThat(comment.getId(), is(notNullValue()));
    assertThat(UUID.fromString(comment.getId()).toString(), is(comment.getId()));
  }

  @Test
  public void should_generate_a_different_id_for_each_comment() {
    Comment first = new Comment("body", "user-1", "article-1");
    Comment second = new Comment("body", "user-1", "article-1");

    assertThat(first.getId(), is(not(second.getId())));
  }

  @Test
  public void should_set_created_at_to_current_time() {
    DateTime before = new DateTime().minusSeconds(1);
    Comment comment = new Comment("body", "user-1", "article-1");
    DateTime after = new DateTime().plusSeconds(1);

    assertThat(comment.getCreatedAt(), is(notNullValue()));
    assertThat(comment.getCreatedAt().isAfter(before), is(true));
    assertThat(comment.getCreatedAt().isBefore(after), is(true));
  }

  @Test
  public void should_accept_null_and_empty_values() {
    Comment comment = new Comment("", null, null);

    assertThat(comment.getBody(), is(""));
    assertThat(comment.getUserId(), is((String) null));
    assertThat(comment.getArticleId(), is((String) null));
    assertThat(comment.getId(), is(notNullValue()));
    assertThat(comment.getCreatedAt(), is(notNullValue()));
  }

  @Test
  public void should_leave_all_fields_null_with_default_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId(), is((String) null));
    assertThat(comment.getBody(), is((String) null));
    assertThat(comment.getUserId(), is((String) null));
    assertThat(comment.getArticleId(), is((String) null));
    assertThat(comment.getCreatedAt(), is((DateTime) null));
  }

  @Test
  public void should_be_equal_to_itself() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertThat(comment.equals(comment), is(true));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }

  @Test
  public void should_not_be_equal_to_another_comment_with_same_content_but_other_id() {
    Comment first = new Comment("body", "user-1", "article-1");
    Comment second = new Comment("body", "user-1", "article-1");

    assertNotEquals(first, second);
  }

  @Test
  public void should_only_use_id_for_equality() {
    Comment first = new Comment("body", "user-1", "article-1");
    Comment second = new Comment("other body", "user-2", "article-2");
    setId(second, first.getId());

    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    Comment comment = new Comment("body", "user-1", "article-1");

    assertThat(comment.equals(null), is(false));
    assertThat(comment.equals("not a comment"), is(false));
  }

  @Test
  public void should_be_equal_when_both_ids_are_null() {
    Comment first = new Comment();
    Comment second = new Comment();

    assertThat(first, is(second));
    assertThat(first.hashCode(), is(second.hashCode()));
  }

  private void setId(Comment comment, String id) {
    try {
      Field field = Comment.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(comment, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
