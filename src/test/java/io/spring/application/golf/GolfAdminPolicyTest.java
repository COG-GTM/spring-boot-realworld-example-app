package io.spring.application.golf;

import io.spring.core.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GolfAdminPolicyTest {
  private static User user(String username) {
    return new User(username + "@example.com", username, "pw", "", "");
  }

  @Test
  public void should_allow_only_configured_usernames() {
    GolfAdminPolicy policy = new GolfAdminPolicy(" golfadmin , other ");
    Assertions.assertTrue(policy.isAdmin(user("golfadmin")));
    Assertions.assertTrue(policy.isAdmin(user("other")));
    Assertions.assertFalse(policy.isAdmin(user("golfdemo1")));
    Assertions.assertFalse(policy.isAdmin(null));
  }

  @Test
  public void should_allow_nobody_when_unconfigured() {
    Assertions.assertFalse(new GolfAdminPolicy("").isAdmin(user("golfadmin")));
  }
}
