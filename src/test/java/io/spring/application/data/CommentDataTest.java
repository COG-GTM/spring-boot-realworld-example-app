package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentDataTest {

  private CommentData sampleComment(DateTime createdAt, DateTime updatedAt) {
    return new CommentData(
        "comment-id",
        "a comment body",
        "article-id",
        createdAt,
        updatedAt,
        new ProfileData("user-id", "jane", "bio", "image", true));
  }

  @Test
  public void should_expose_all_constructor_values() {
    DateTime createdAt = new DateTime(1000L);
    DateTime updatedAt = new DateTime(2000L);

    CommentData commentData = sampleComment(createdAt, updatedAt);

    assertThat(commentData.getId()).isEqualTo("comment-id");
    assertThat(commentData.getBody()).isEqualTo("a comment body");
    assertThat(commentData.getArticleId()).isEqualTo("article-id");
    assertThat(commentData.getCreatedAt()).isEqualTo(createdAt);
    assertThat(commentData.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(commentData.getProfileData().isFollowing()).isTrue();
  }

  @Test
  public void should_build_cursor_from_created_at_not_updated_at() {
    CommentData commentData = sampleComment(new DateTime(1000L), new DateTime(9999L));

    DateTimeCursor cursor = commentData.getCursor();

    assertThat(cursor.getData()).isEqualTo(new DateTime(1000L));
    assertThat(cursor.toString()).isEqualTo("1000");
  }

  @Test
  public void should_reflect_setter_changes_in_cursor() {
    CommentData commentData = new CommentData();
    commentData.setCreatedAt(new DateTime(555L));
    commentData.setBody("updated body");

    assertThat(commentData.getCursor().toString()).isEqualTo("555");
    assertThat(commentData.getBody()).isEqualTo("updated body");
  }

  @Test
  public void should_be_equal_for_same_values() {
    DateTime createdAt = new DateTime(1000L);
    DateTime updatedAt = new DateTime(2000L);

    CommentData one = sampleComment(createdAt, updatedAt);
    CommentData other = sampleComment(createdAt, updatedAt);

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("comment-id");

    other.setBody("different");
    assertThat(one).isNotEqualTo(other);
  }
}
