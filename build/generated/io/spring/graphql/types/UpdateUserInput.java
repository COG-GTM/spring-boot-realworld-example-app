package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class UpdateUserInput {
  private String email;

  private String username;

  private String password;

  private String image;

  private String bio;

  public UpdateUserInput() {
  }

  public UpdateUserInput(String email, String username, String password, String image, String bio) {
    this.email = email;
    this.username = username;
    this.password = password;
    this.image = image;
    this.bio = bio;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  @Override
  public String toString() {
    return "UpdateUserInput{" + "email='" + email + "'," +"username='" + username + "'," +"password='" + password + "'," +"image='" + image + "'," +"bio='" + bio + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateUserInput that = (UpdateUserInput) o;
        return java.util.Objects.equals(email, that.email) &&
                            java.util.Objects.equals(username, that.username) &&
                            java.util.Objects.equals(password, that.password) &&
                            java.util.Objects.equals(image, that.image) &&
                            java.util.Objects.equals(bio, that.bio);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(email, username, password, image, bio);
  }

  public static io.spring.graphql.types.UpdateUserInput.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String email;

    private String username;

    private String password;

    private String image;

    private String bio;

    public UpdateUserInput build() {
                  io.spring.graphql.types.UpdateUserInput result = new io.spring.graphql.types.UpdateUserInput();
                      result.email = this.email;
          result.username = this.username;
          result.password = this.password;
          result.image = this.image;
          result.bio = this.bio;
                      return result;
    }

    public io.spring.graphql.types.UpdateUserInput.Builder email(String email) {
      this.email = email;
      return this;
    }

    public io.spring.graphql.types.UpdateUserInput.Builder username(String username) {
      this.username = username;
      return this;
    }

    public io.spring.graphql.types.UpdateUserInput.Builder password(String password) {
      this.password = password;
      return this;
    }

    public io.spring.graphql.types.UpdateUserInput.Builder image(String image) {
      this.image = image;
      return this;
    }

    public io.spring.graphql.types.UpdateUserInput.Builder bio(String bio) {
      this.bio = bio;
      return this;
    }
  }
}
