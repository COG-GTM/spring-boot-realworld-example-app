package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentDataTest {

  @Test
  public void should_create_comment_data() {
    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData("id123", "testuser", "bio", "image.jpg", false);
    CommentData commentData = new CommentData("comment123", "test body", "article456", now, now, profileData);
    
    assertThat(commentData.getId(), is("comment123"));
    assertThat(commentData.getBody(), is("test body"));
    assertThat(commentData.getArticleId(), is("article456"));
    assertThat(commentData.getCreatedAt(), is(now));
    assertThat(commentData.getUpdatedAt(), is(now));
    assertThat(commentData.getProfileData(), is(profileData));
  }

  @Test
  public void should_get_cursor() {
    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData("id123", "testuser", "bio", "image.jpg", false);
    CommentData commentData = new CommentData("comment123", "test body", "article456", now, now, profileData);
    
    assertThat(commentData.getCursor(), notNullValue());
  }

  @Test
  public void should_have_equals_and_hashcode() {
    DateTime now = new DateTime();
    ProfileData profileData = new ProfileData("id123", "testuser", "bio", "image.jpg", false);
    CommentData commentData1 = new CommentData("comment123", "test body", "article456", now, now, profileData);
    CommentData commentData2 = new CommentData("comment123", "test body", "article456", now, now, profileData);
    
    assertThat(commentData1.equals(commentData2), is(true));
    assertThat(commentData1.hashCode(), is(commentData2.hashCode()));
  }
}
