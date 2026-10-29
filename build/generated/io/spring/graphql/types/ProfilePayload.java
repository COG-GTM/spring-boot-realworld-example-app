package io.spring.graphql.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class ProfilePayload {
  private Profile profile;

  public ProfilePayload() {
  }

  public ProfilePayload(Profile profile) {
    this.profile = profile;
  }

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  @Override
  public String toString() {
    return "ProfilePayload{" + "profile='" + profile + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfilePayload that = (ProfilePayload) o;
        return java.util.Objects.equals(profile, that.profile);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(profile);
  }

  public static io.spring.graphql.types.ProfilePayload.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Profile profile;

    public ProfilePayload build() {
      io.spring.graphql.types.ProfilePayload result = new io.spring.graphql.types.ProfilePayload();
          result.profile = this.profile;
          return result;
    }

    public io.spring.graphql.types.ProfilePayload.Builder profile(Profile profile) {
      this.profile = profile;
      return this;
    }
  }
}
