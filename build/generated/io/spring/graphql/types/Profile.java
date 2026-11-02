package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Profile {
  private String username;

  private String bio;

  private boolean following;

  private String image;

  private ArticlesConnection articles;

  private ArticlesConnection favorites;

  private ArticlesConnection feed;

  public Profile() {
  }

  public Profile(String username, String bio, boolean following, String image,
      ArticlesConnection articles, ArticlesConnection favorites, ArticlesConnection feed) {
    this.username = username;
    this.bio = bio;
    this.following = following;
    this.image = image;
    this.articles = articles;
    this.favorites = favorites;
    this.feed = feed;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public boolean getFollowing() {
    return following;
  }

  public void setFollowing(boolean following) {
    this.following = following;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public ArticlesConnection getArticles() {
    return articles;
  }

  public void setArticles(ArticlesConnection articles) {
    this.articles = articles;
  }

  public ArticlesConnection getFavorites() {
    return favorites;
  }

  public void setFavorites(ArticlesConnection favorites) {
    this.favorites = favorites;
  }

  public ArticlesConnection getFeed() {
    return feed;
  }

  public void setFeed(ArticlesConnection feed) {
    this.feed = feed;
  }

  @Override
  public String toString() {
    return "Profile{" + "username='" + username + "'," +"bio='" + bio + "'," +"following='" + following + "'," +"image='" + image + "'," +"articles='" + articles + "'," +"favorites='" + favorites + "'," +"feed='" + feed + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile that = (Profile) o;
        return java.util.Objects.equals(username, that.username) &&
                            java.util.Objects.equals(bio, that.bio) &&
                            following == that.following &&
                            java.util.Objects.equals(image, that.image) &&
                            java.util.Objects.equals(articles, that.articles) &&
                            java.util.Objects.equals(favorites, that.favorites) &&
                            java.util.Objects.equals(feed, that.feed);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(username, bio, following, image, articles, favorites, feed);
  }

  public static io.spring.graphql.types.Profile.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String username;

    private String bio;

    private boolean following;

    private String image;

    private ArticlesConnection articles;

    private ArticlesConnection favorites;

    private ArticlesConnection feed;

    public Profile build() {
                  io.spring.graphql.types.Profile result = new io.spring.graphql.types.Profile();
                      result.username = this.username;
          result.bio = this.bio;
          result.following = this.following;
          result.image = this.image;
          result.articles = this.articles;
          result.favorites = this.favorites;
          result.feed = this.feed;
                      return result;
    }

    public io.spring.graphql.types.Profile.Builder username(String username) {
      this.username = username;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder bio(String bio) {
      this.bio = bio;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder following(boolean following) {
      this.following = following;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder image(String image) {
      this.image = image;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder articles(ArticlesConnection articles) {
      this.articles = articles;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder favorites(ArticlesConnection favorites) {
      this.favorites = favorites;
      return this;
    }

    public io.spring.graphql.types.Profile.Builder feed(ArticlesConnection feed) {
      this.feed = feed;
      return this;
    }
  }
}
