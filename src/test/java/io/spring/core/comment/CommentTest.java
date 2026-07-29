package io.spring.core.comment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  public void should_set_all_fields_on_construction() {
    Comment comment = new Comment("comment body", "user-id", "article-id");

    assertThat(comment.getBody(), is("comment body"));
    assertThat(comment.getUserId(), is("user-id"));
    assertThat(comment.getArticleId(), is("article-id"));
  }

  @Test
  public void should_generate_uuid_as_id() {
    Comment comment = new Comment("comment body", "user-id", "article-id");

    assertThat(comment.getId(), is(notNullValue()));
    assertThat(UUID.fromString(comment.getId()).toString(), is(comment.getId()));
  }

  @Test
  public void should_generate_different_id_for_each_comment() {
    Comment one = new Comment("comment body", "user-id", "article-id");
    Comment another = new Comment("comment body", "user-id", "article-id");

    assertThat(one.getId(), is(not(another.getId())));
  }

  @Test
  public void should_initialize_created_at_to_now() {
    DateTime before = new DateTime().minusSeconds(1);

    Comment comment = new Comment("comment body", "user-id", "article-id");

    DateTime after = new DateTime().plusSeconds(1);
    assertThat(comment.getCreatedAt(), is(notNullValue()));
    assertThat(comment.getCreatedAt().isAfter(before), is(true));
    assertThat(comment.getCreatedAt().isBefore(after), is(true));
  }

  @Test
  public void should_leave_fields_null_with_default_constructor() {
    Comment comment = new Comment();

    assertThat(comment.getId(), is((String) null));
    assertThat(comment.getBody(), is((String) null));
    assertThat(comment.getUserId(), is((String) null));
    assertThat(comment.getArticleId(), is((String) null));
    assertThat(comment.getCreatedAt(), is((DateTime) null));
  }

  @Test
  public void should_be_equal_when_id_is_equal() {
    Comment comment = new Comment("comment body", "user-id", "article-id");
    Comment sameId = new Comment("other body", "other-user", "other-article");
    setId(sameId, comment.getId());

    assertThat(comment.equals(sameId), is(true));
    assertThat(comment.hashCode(), is(sameId.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_id_differs() {
    Comment comment = new Comment("comment body", "user-id", "article-id");
    Comment another = new Comment("comment body", "user-id", "article-id");

    assertThat(comment.equals(another), is(false));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    Comment comment = new Comment("comment body", "user-id", "article-id");

    assertThat(comment.equals(null), is(false));
    assertThat(comment.equals("comment body"), is(false));
  }

  @Test
  public void should_be_equal_to_itself() {
    Comment comment = new Comment("comment body", "user-id", "article-id");

    assertThat(comment.equals(comment), is(true));
    assertThat(comment.hashCode(), is(comment.hashCode()));
  }

  private void setId(Comment comment, String id) {
    try {
      java.lang.reflect.Field field = Comment.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(comment, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
