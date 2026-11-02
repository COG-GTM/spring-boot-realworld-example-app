package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class User {
  private String email;

  private Profile profile;

  private String token;

  private String username;

  public User() {
  }

  public User(String email, Profile profile, String token, String username) {
    this.email = email;
    this.profile = profile;
    this.token = token;
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    return "User{" + "email='" + email + "'," +"profile='" + profile + "'," +"token='" + token + "'," +"username='" + username + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return java.util.Objects.equals(email, that.email) &&
                            java.util.Objects.equals(profile, that.profile) &&
                            java.util.Objects.equals(token, that.token) &&
                            java.util.Objects.equals(username, that.username);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(email, profile, token, username);
  }

  public static io.spring.graphql.types.User.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String email;

    private Profile profile;

    private String token;

    private String username;

    public User build() {
                  io.spring.graphql.types.User result = new io.spring.graphql.types.User();
                      result.email = this.email;
          result.profile = this.profile;
          result.token = this.token;
          result.username = this.username;
                      return result;
    }

    public io.spring.graphql.types.User.Builder email(String email) {
      this.email = email;
      return this;
    }

    public io.spring.graphql.types.User.Builder profile(Profile profile) {
      this.profile = profile;
      return this;
    }

    public io.spring.graphql.types.User.Builder token(String token) {
      this.token = token;
      return this;
    }

    public io.spring.graphql.types.User.Builder username(String username) {
      this.username = username;
      return this;
    }
  }
}
