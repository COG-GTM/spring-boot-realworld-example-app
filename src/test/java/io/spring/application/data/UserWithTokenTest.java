package io.spring.application.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserWithTokenTest {

  @Test
  public void should_create_user_with_token_from_user_data() {
    UserData userData =
        new UserData("user-id", "test@example.com", "testuser", "Test bio", "http://image.url");
    String token = "jwt-token-123";

    UserWithToken userWithToken = new UserWithToken(userData, token);

    assertThat(userWithToken.getEmail(), is("test@example.com"));
    assertThat(userWithToken.getUsername(), is("testuser"));
    assertThat(userWithToken.getBio(), is("Test bio"));
    assertThat(userWithToken.getImage(), is("http://image.url"));
    assertThat(userWithToken.getToken(), is("jwt-token-123"));
  }

  @Test
  public void should_handle_null_fields_in_user_data() {
    UserData userData = new UserData(null, null, null, null, null);
    String token = "jwt-token-123";

    UserWithToken userWithToken = new UserWithToken(userData, token);

    assertThat(userWithToken.getEmail(), is((String) null));
    assertThat(userWithToken.getUsername(), is((String) null));
    assertThat(userWithToken.getBio(), is((String) null));
    assertThat(userWithToken.getImage(), is((String) null));
    assertThat(userWithToken.getToken(), is("jwt-token-123"));
  }

  @Test
  public void should_create_user_with_token_with_empty_strings() {
    UserData userData = new UserData("user-id", "", "", "", "");
    String token = "";

    UserWithToken userWithToken = new UserWithToken(userData, token);

    assertThat(userWithToken.getEmail(), is(""));
    assertThat(userWithToken.getUsername(), is(""));
    assertThat(userWithToken.getBio(), is(""));
    assertThat(userWithToken.getImage(), is(""));
    assertThat(userWithToken.getToken(), is(""));
  }
}
