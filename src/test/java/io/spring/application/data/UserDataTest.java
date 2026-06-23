package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserDataTest {

  private UserData full() {
    return new UserData("id", "jake@test.com", "jake", "bio", "img");
  }

  @Test
  void allArgsConstructorAndGettersExposeEveryField() {
    UserData data = full();

    assertThat(data.getId()).isEqualTo("id");
    assertThat(data.getEmail()).isEqualTo("jake@test.com");
    assertThat(data.getUsername()).isEqualTo("jake");
    assertThat(data.getBio()).isEqualTo("bio");
    assertThat(data.getImage()).isEqualTo("img");
  }

  @Test
  void noArgsConstructorAndSettersMutateState() {
    UserData data = new UserData();

    data.setId("id2");
    data.setEmail("john@test.com");
    data.setUsername("john");
    data.setBio(null);
    data.setImage(null);

    assertThat(data.getId()).isEqualTo("id2");
    assertThat(data.getEmail()).isEqualTo("john@test.com");
    assertThat(data.getUsername()).isEqualTo("john");
    assertThat(data.getBio()).isNull();
    assertThat(data.getImage()).isNull();
  }

  @Test
  void equalsAndHashCodeHoldForIdenticalContent() {
    UserData a = full();
    UserData b = full();

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isEqualTo(a);
    assertThat(a).isNotEqualTo(null).isNotEqualTo("string");
  }

  @Test
  void equalsDistinguishesEveryField() {
    UserData base = full();

    assertThat(base).isNotEqualTo(mutate(d -> d.setId("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setEmail("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setUsername("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setBio("x")));
    assertThat(base).isNotEqualTo(mutate(d -> d.setImage("x")));
  }

  @Test
  void toStringContainsKeyFields() {
    assertThat(full().toString()).contains("UserData", "email=jake@test.com", "username=jake");
  }

  @Test
  void userWithTokenIsBuiltFromUserData() {
    UserWithToken withToken = new UserWithToken(full(), "jwt-token");

    assertThat(withToken.getEmail()).isEqualTo("jake@test.com");
    assertThat(withToken.getUsername()).isEqualTo("jake");
    assertThat(withToken.getBio()).isEqualTo("bio");
    assertThat(withToken.getImage()).isEqualTo("img");
    assertThat(withToken.getToken()).isEqualTo("jwt-token");
  }

  private UserData mutate(java.util.function.Consumer<UserData> change) {
    UserData copy = full();
    change.accept(copy);
    return copy;
  }
}
