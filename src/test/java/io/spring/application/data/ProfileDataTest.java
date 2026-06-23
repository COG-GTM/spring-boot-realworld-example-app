package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfileDataTest {

  private ProfileData full() {
    return new ProfileData("id", "jake", "bio", "img", true);
  }

  @Test
  void allArgsConstructorAndGettersExposeEveryField() {
    ProfileData data = full();

    assertThat(data.getId()).isEqualTo("id");
    assertThat(data.getUsername()).isEqualTo("jake");
    assertThat(data.getBio()).isEqualTo("bio");
    assertThat(data.getImage()).isEqualTo("img");
    assertThat(data.isFollowing()).isTrue();
  }

  @Test
  void noArgsConstructorAndSettersMutateState() {
    ProfileData data = new ProfileData();

    data.setId("id2");
    data.setUsername("john");
    data.setBio(null);
    data.setImage(null);
    data.setFollowing(false);

    assertThat(data.getId()).isEqualTo("id2");
    assertThat(data.getUsername()).isEqualTo("john");
    assertThat(data.getBio()).isNull();
    assertThat(data.getImage()).isNull();
    assertThat(data.isFollowing()).isFalse();
  }

  @Test
  void equalsAndHashCodeHoldForIdenticalContent() {
    ProfileData a = full();
    ProfileData b = full();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(a);
    assertThat(a).isNotEqualTo(null).isNotEqualTo("string");
  }

  @Test
  void equalsDistinguishesEveryField() {
    ProfileData base = full();

    assertThat(base).isNotEqualTo(mutate(d -> d.setId("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setUsername("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setBio("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setImage("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setFollowing(false)));
  }

  @Test
  void toStringContainsKeyFields() {
    assertThat(full().toString()).contains("ProfileData", "username=jake", "following=true");
  }

  private ProfileData mutate(java.util.function.Consumer<ProfileData> change) {
    ProfileData copy = full();
    change.accept(copy);
    return copy;
  }
}
