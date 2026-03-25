package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.application.DateTimeCursor;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentDataCoverageTest {

  @Test
  public void should_create_with_all_args() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    CommentData data = new CommentData("id1", "body", "articleId", now, now, profile);

    assertEquals("id1", data.getId());
    assertEquals("body", data.getBody());
    assertEquals("articleId", data.getArticleId());
    assertEquals(now, data.getCreatedAt());
    assertEquals(now, data.getUpdatedAt());
    assertEquals(profile, data.getProfileData());
  }

  @Test
  public void should_create_with_no_arg_constructor() {
    CommentData data = new CommentData();
    assertNull(data.getId());
    assertNull(data.getBody());
  }

  @Test
  public void should_get_cursor() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    CommentData data = new CommentData("id1", "body", "articleId", now, now, profile);

    DateTimeCursor cursor = data.getCursor();
    assertNotNull(cursor);
    assertEquals(now, cursor.getData());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", false);
    CommentData data1 = new CommentData("id1", "body", "articleId", now, now, profile);
    CommentData data2 = new CommentData("id1", "body", "articleId", now, now, profile);

    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());
  }

  @Test
  public void should_set_profile_data() {
    CommentData data = new CommentData();
    ProfileData profile = new ProfileData("id", "user", "bio", "img", true);
    data.setProfileData(profile);
    assertEquals(profile, data.getProfileData());
    assertTrue(data.getProfileData().isFollowing());
  }
}
