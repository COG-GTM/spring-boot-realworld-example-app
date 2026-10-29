package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CreateUserInput {
  private String email;

  private String username;

  private String password;

  public CreateUserInput() {
  }

  public CreateUserInput(String email, String username, String password) {
    this.email = email;
    this.username = username;
    this.password = password;
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

  @Override
  public String toString() {
    return "CreateUserInput{" + "email='" + email + "'," +"username='" + username + "'," +"password='" + password + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateUserInput that = (CreateUserInput) o;
        return java.util.Objects.equals(email, that.email) &&
                            java.util.Objects.equals(username, that.username) &&
                            java.util.Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(email, username, password);
  }

  public static io.spring.graphql.types.CreateUserInput.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String email;

    private String username;

    private String password;

    public CreateUserInput build() {
                  io.spring.graphql.types.CreateUserInput result = new io.spring.graphql.types.CreateUserInput();
                      result.email = this.email;
          result.username = this.username;
          result.password = this.password;
                      return result;
    }

    public io.spring.graphql.types.CreateUserInput.Builder email(String email) {
      this.email = email;
      return this;
    }

    public io.spring.graphql.types.CreateUserInput.Builder username(String username) {
      this.username = username;
      return this;
    }

    public io.spring.graphql.types.CreateUserInput.Builder password(String password) {
      this.password = password;
      return this;
    }
  }
}
