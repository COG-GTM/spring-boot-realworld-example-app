package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ProfileTypeTest {
  private ArticlesConnection connection(String cursor) {
    return ArticlesConnection.newBuilder()
        .edges(Collections.singletonList(ArticleEdge.newBuilder().cursor(cursor).build()))
        .build();
  }

  @Test
  public void should_build_profile_with_all_fields() {
    ArticlesConnection articles = connection("a");
    ArticlesConnection favorites = connection("f");
    ArticlesConnection feed = connection("d");

    Profile profile =
        Profile.newBuilder()
            .username("jack")
            .bio("bio")
            .following(true)
            .image("image.png")
            .articles(articles)
            .favorites(favorites)
            .feed(feed)
            .build();

    assertThat(profile.getUsername()).isEqualTo("jack");
    assertThat(profile.getBio()).isEqualTo("bio");
    assertThat(profile.getFollowing()).isTrue();
    assertThat(profile.getImage()).isEqualTo("image.png");
    assertThat(profile.getArticles()).isSameAs(articles);
    assertThat(profile.getFavorites()).isSameAs(favorites);
    assertThat(profile.getFeed()).isSameAs(feed);
  }

  @Test
  public void should_construct_profile_with_all_args_constructor() {
    ArticlesConnection articles = connection("a");

    Profile profile = new Profile("jack", "bio", false, "image.png", articles, null, null);

    assertThat(profile.getUsername()).isEqualTo("jack");
    assertThat(profile.getBio()).isEqualTo("bio");
    assertThat(profile.getFollowing()).isFalse();
    assertThat(profile.getImage()).isEqualTo("image.png");
    assertThat(profile.getArticles()).isSameAs(articles);
    assertThat(profile.getFavorites()).isNull();
    assertThat(profile.getFeed()).isNull();
  }

  @Test
  public void should_set_fields_with_setters() {
    Profile profile = new Profile();
    assertThat(profile.getUsername()).isNull();
    assertThat(profile.getFollowing()).isFalse();

    ArticlesConnection favorites = connection("f");
    ArticlesConnection feed = connection("d");
    ArticlesConnection articles = connection("a");
    profile.setUsername("jill");
    profile.setBio("new bio");
    profile.setFollowing(true);
    profile.setImage("avatar.png");
    profile.setArticles(articles);
    profile.setFavorites(favorites);
    profile.setFeed(feed);

    assertThat(profile.getUsername()).isEqualTo("jill");
    assertThat(profile.getBio()).isEqualTo("new bio");
    assertThat(profile.getFollowing()).isTrue();
    assertThat(profile.getImage()).isEqualTo("avatar.png");
    assertThat(profile.getArticles()).isSameAs(articles);
    assertThat(profile.getFavorites()).isSameAs(favorites);
    assertThat(profile.getFeed()).isSameAs(feed);
  }

  @Test
  public void should_render_fields_in_to_string() {
    Profile profile = new Profile("jack", "bio", true, "image.png", null, null, null);

    assertThat(profile.toString())
        .startsWith("Profile{")
        .contains("username='jack'")
        .contains("bio='bio'")
        .contains("following='true'")
        .contains("image='image.png'");
  }

  @Test
  public void should_compare_by_value() {
    Profile one = new Profile("jack", "bio", true, "image.png", null, null, null);
    Profile same = new Profile("jack", "bio", true, "image.png", null, null, null);
    Profile other = new Profile("jill", "bio", true, "image.png", null, null, null);

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals(new Object())).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
