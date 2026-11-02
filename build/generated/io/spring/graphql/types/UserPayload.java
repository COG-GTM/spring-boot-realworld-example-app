package io.spring.graphql.types;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NONE
)
public class UserPayload implements UserResult {
  private User user;

  public UserPayload() {
  }

  public UserPayload(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public String toString() {
    return "UserPayload{" + "user='" + user + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPayload that = (UserPayload) o;
        return java.util.Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(user);
  }

  public static io.spring.graphql.types.UserPayload.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private User user;

    public UserPayload build() {
      io.spring.graphql.types.UserPayload result = new io.spring.graphql.types.UserPayload();
          result.user = this.user;
          return result;
    }

    public io.spring.graphql.types.UserPayload.Builder user(User user) {
      this.user = user;
      return this;
    }
  }
}
