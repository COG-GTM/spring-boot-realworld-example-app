package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class DataClassesExtendedTest {

  // ===== ArticleData Lombok-generated methods =====

  @Test
  public void article_data_equals_same_object() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    assertEquals(a, a);
  }

  @Test
  public void article_data_equals_same_values() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a1 = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    ArticleData a2 = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  @Test
  public void article_data_not_equals_different_values() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a1 = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    ArticleData a2 = new ArticleData("a2", "slug2", "Title2", "desc2", "body2", true, 5, now, now, Collections.emptyList(), profile);
    assertNotEquals(a1, a2);
  }

  @Test
  public void article_data_not_equals_null() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    assertNotEquals(null, a);
  }

  @Test
  public void article_data_not_equals_other_type() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    assertNotEquals("string", a);
  }

  @Test
  public void article_data_toString() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData a = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    String str = a.toString();
    assertNotNull(str);
    assertTrue(str.contains("a1"));
    assertTrue(str.contains("slug"));
  }

  @Test
  public void article_data_no_arg_constructor_and_setters() {
    ArticleData a = new ArticleData();
    a.setId("id");
    a.setSlug("slug");
    a.setTitle("title");
    a.setDescription("desc");
    a.setBody("body");
    a.setFavorited(true);
    a.setFavoritesCount(5);
    DateTime now = new DateTime();
    a.setCreatedAt(now);
    a.setUpdatedAt(now);
    a.setTagList(Arrays.asList("java"));
    ProfileData p = new ProfileData();
    a.setProfileData(p);
    assertEquals("id", a.getId());
    assertEquals("slug", a.getSlug());
    assertEquals("title", a.getTitle());
    assertEquals("desc", a.getDescription());
    assertEquals("body", a.getBody());
    assertTrue(a.isFavorited());
    assertEquals(5, a.getFavoritesCount());
    assertEquals(now, a.getCreatedAt());
    assertEquals(now, a.getUpdatedAt());
    assertEquals(1, a.getTagList().size());
    assertNotNull(a.getProfileData());
  }

  @Test
  public void article_data_canEqual() {
    ArticleData a1 = new ArticleData();
    ArticleData a2 = new ArticleData();
    assertTrue(a1.canEqual(a2));
    assertFalse(a1.canEqual("string"));
  }

  // ===== CommentData Lombok-generated methods =====

  @Test
  public void comment_data_equals_same_values() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    CommentData c1 = new CommentData("c1", "body", "a1", now, now, profile);
    CommentData c2 = new CommentData("c1", "body", "a1", now, now, profile);
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  public void comment_data_not_equals_different() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    CommentData c1 = new CommentData("c1", "body1", "a1", now, now, profile);
    CommentData c2 = new CommentData("c2", "body2", "a2", now, now, profile);
    assertNotEquals(c1, c2);
  }

  @Test
  public void comment_data_toString() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    CommentData c = new CommentData("c1", "body", "a1", now, now, profile);
    assertNotNull(c.toString());
    assertTrue(c.toString().contains("c1"));
  }

  @Test
  public void comment_data_no_arg_constructor_and_setters() {
    CommentData c = new CommentData();
    c.setId("id");
    c.setBody("body");
    c.setArticleId("a1");
    DateTime now = new DateTime();
    c.setCreatedAt(now);
    c.setUpdatedAt(now);
    ProfileData p = new ProfileData();
    c.setProfileData(p);
    assertEquals("id", c.getId());
    assertEquals("body", c.getBody());
    assertEquals("a1", c.getArticleId());
    assertEquals(now, c.getCreatedAt());
    assertEquals(now, c.getUpdatedAt());
    assertNotNull(c.getProfileData());
  }

  @Test
  public void comment_data_canEqual() {
    CommentData c1 = new CommentData();
    CommentData c2 = new CommentData();
    assertTrue(c1.canEqual(c2));
    assertFalse(c1.canEqual("string"));
  }

  @Test
  public void comment_data_not_equals_null_and_other_type() {
    CommentData c = new CommentData();
    assertNotEquals(null, c);
    assertNotEquals("string", c);
  }

  // ===== UserData Lombok-generated methods =====

  @Test
  public void user_data_equals_same_values() {
    UserData u1 = new UserData("id1", "email@test.com", "user1", "bio", "image");
    UserData u2 = new UserData("id1", "email@test.com", "user1", "bio", "image");
    assertEquals(u1, u2);
    assertEquals(u1.hashCode(), u2.hashCode());
  }

  @Test
  public void user_data_not_equals_different() {
    UserData u1 = new UserData("id1", "e1@test.com", "user1", "bio1", "img1");
    UserData u2 = new UserData("id2", "e2@test.com", "user2", "bio2", "img2");
    assertNotEquals(u1, u2);
  }

  @Test
  public void user_data_toString() {
    UserData u = new UserData("id1", "email@test.com", "user1", "bio", "image");
    assertNotNull(u.toString());
    assertTrue(u.toString().contains("id1"));
  }

  @Test
  public void user_data_no_arg_constructor_and_setters() {
    UserData u = new UserData();
    u.setId("id");
    u.setEmail("e@t.com");
    u.setUsername("user");
    u.setBio("bio");
    u.setImage("img");
    assertEquals("id", u.getId());
    assertEquals("e@t.com", u.getEmail());
    assertEquals("user", u.getUsername());
    assertEquals("bio", u.getBio());
    assertEquals("img", u.getImage());
  }

  @Test
  public void user_data_canEqual() {
    UserData u1 = new UserData();
    UserData u2 = new UserData();
    assertTrue(u1.canEqual(u2));
    assertFalse(u1.canEqual("string"));
  }

  @Test
  public void user_data_not_equals_null_and_other_type() {
    UserData u = new UserData();
    assertNotEquals(null, u);
    assertNotEquals("string", u);
  }

  // ===== ProfileData Lombok-generated methods =====

  @Test
  public void profile_data_equals_same_values() {
    ProfileData p1 = new ProfileData("id1", "user1", "bio", "image", true);
    ProfileData p2 = new ProfileData("id1", "user1", "bio", "image", true);
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
  }

  @Test
  public void profile_data_not_equals_different() {
    ProfileData p1 = new ProfileData("id1", "user1", "bio1", "img1", true);
    ProfileData p2 = new ProfileData("id2", "user2", "bio2", "img2", false);
    assertNotEquals(p1, p2);
  }

  @Test
  public void profile_data_toString() {
    ProfileData p = new ProfileData("id1", "user1", "bio", "image", true);
    assertNotNull(p.toString());
    assertTrue(p.toString().contains("user1"));
  }

  @Test
  public void profile_data_no_arg_constructor_and_setters() {
    ProfileData p = new ProfileData();
    p.setId("id");
    p.setUsername("user");
    p.setBio("bio");
    p.setImage("img");
    p.setFollowing(true);
    assertEquals("id", p.getId());
    assertEquals("user", p.getUsername());
    assertEquals("bio", p.getBio());
    assertEquals("img", p.getImage());
    assertTrue(p.isFollowing());
  }

  @Test
  public void profile_data_canEqual() {
    ProfileData p1 = new ProfileData();
    ProfileData p2 = new ProfileData();
    assertTrue(p1.canEqual(p2));
    assertFalse(p1.canEqual("string"));
  }

  @Test
  public void profile_data_not_equals_null_and_other_type() {
    ProfileData p = new ProfileData();
    assertNotEquals(null, p);
    assertNotEquals("string", p);
  }

  // ===== ArticleFavoriteCount Lombok-generated methods (uses @Value) =====

  @Test
  public void article_favorite_count_equals_same_values() {
    ArticleFavoriteCount a1 = new ArticleFavoriteCount("id1", 10);
    ArticleFavoriteCount a2 = new ArticleFavoriteCount("id1", 10);
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  @Test
  public void article_favorite_count_not_equals_different() {
    ArticleFavoriteCount a1 = new ArticleFavoriteCount("id1", 10);
    ArticleFavoriteCount a2 = new ArticleFavoriteCount("id2", 20);
    assertNotEquals(a1, a2);
  }

  @Test
  public void article_favorite_count_toString() {
    ArticleFavoriteCount a = new ArticleFavoriteCount("id1", 10);
    assertNotNull(a.toString());
    assertTrue(a.toString().contains("id1"));
  }

  @Test
  public void article_favorite_count_not_equals_null_and_other_type() {
    ArticleFavoriteCount a = new ArticleFavoriteCount("id1", 10);
    assertNotEquals(null, a);
    assertNotEquals("string", a);
  }

  // ===== ArticleDataList =====

  @Test
  public void article_data_list_getters() {
    DateTime now = new DateTime();
    ProfileData profile = new ProfileData("id1", "user1", "bio", "image", false);
    ArticleData article = new ArticleData("a1", "slug", "Title", "desc", "body", false, 0, now, now, Collections.emptyList(), profile);
    ArticleDataList list = new ArticleDataList(Arrays.asList(article), 1);
    assertEquals(1, list.getCount());
    assertEquals(1, list.getArticleDatas().size());
    assertEquals("a1", list.getArticleDatas().get(0).getId());
  }

  // ===== UserWithToken =====

  @Test
  public void user_with_token_all_getters() {
    UserData userData = new UserData("id1", "email@test.com", "user1", "bio", "image");
    UserWithToken uwt = new UserWithToken(userData, "token");
    assertEquals("email@test.com", uwt.getEmail());
    assertEquals("user1", uwt.getUsername());
    assertEquals("bio", uwt.getBio());
    assertEquals("image", uwt.getImage());
    assertEquals("token", uwt.getToken());
  }
}
